package com.han.back.domain.device.service.implement;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.global.device.DeviceProperties;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final TokenService tokenService;
    private final DeviceProperties deviceProperties;

    @Override
    @Transactional
    public DeviceRegistration registerLoginDevice(Long userId, DeviceInfo deviceInfo) {
        String sessionId = UuidUtil.generateString();

        Optional<DeviceEntity> registeredDevice =
                deviceRepository.findByUserIdAndDeviceFingerprint(
                        userId, deviceInfo.getDeviceFingerprint());

        boolean isNewDevice = registeredDevice.isEmpty();

        DeviceEntity device = registeredDevice
                .orElseGet(() -> createNewDevice(userId, deviceInfo));

        device.activateSession(
                sessionId,
                deviceInfo.getDeviceType(),
                deviceInfo.getOsName(),
                deviceInfo.getBrowserName(),
                deviceInfo.getLoginIp()
        );

        deviceRepository.save(device);
        enforceMaxSessionPolicy(userId, sessionId);

        log.info("Device Registered - UserPK: {} | DeviceId: {} | Type: {} | SessionId: {} | IP: {} | NewDevice: {}",
                device.getUserId(), device.getId(), deviceInfo.getDeviceType().name(),
                sessionId, deviceInfo.getLoginIp(), isNewDevice);

        return DeviceRegistration.of(sessionId, isNewDevice);
    }

    @Override
    @Transactional
    public void trustDevice(Long userId, String devicePublicId) {
        DeviceEntity device = deviceRepository.findByPublicIdAndUserId(devicePublicId, userId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_FOUND_DEVICE));

        if (device.isTrusted()) return;

        int trustedCount = deviceRepository.countTrustedDevicesByUserId(userId);
        if (trustedCount >= deviceProperties.getMaxTrustedDevices()) {
            throw new CustomException(BaseResponseStatus.TRUSTED_DEVICE_LIMIT_EXCEEDED);
        }

        device.markAsTrusted();

        log.info("Device Trusted - UserPK: {} | DevicePublicId: {}", userId, devicePublicId);
    }

    @Override
    @Transactional
    public void untrustDevice(Long userId, String devicePublicId) {
        DeviceEntity device = deviceRepository.findByPublicIdAndUserId(devicePublicId, userId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_FOUND_DEVICE));

        if (!device.isTrusted()) return;

        device.unmarkTrusted();

        log.info("Device Untrusted - UserPK: {} | DevicePublicId: {}", userId, devicePublicId);
    }

    @Override
    @Transactional
    public String rotateDeviceSession(Long userId, String oldSessionId) {
        DeviceEntity device = deviceRepository.findByUserIdAndSessionId(userId, oldSessionId)
                .orElseThrow(() -> new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL));

        String newSessionId = UuidUtil.generateString();
        device.rotateSession(newSessionId);

        log.debug("Device Session Rotated - UserPK: {} | OldSessionId: {} | NewSessionId: {}",
                userId, oldSessionId, newSessionId);

        return newSessionId;
    }

    @Override
    @Transactional
    public void deactivateSession(Long userId, String sessionId) {
        int updated = deviceRepository.deactivateSessionByUserIdAndSessionId(userId, sessionId);

        if (updated > 0) {
            log.debug("Device Session Deactivated - UserPK: {} | SessionId: {}", userId, sessionId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceDetailResponseDto> getDevices(Long userId, String currentSessionId) {
        List<DeviceEntity> devices = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(userId);

        return devices.stream()
                .map(device -> DeviceDetailResponseDto.from(device, currentSessionId))
                .toList();
    }

    @Override
    @Transactional
    public void forceLogoutDevice(Long userId, String devicePublicId, String currentSessionId) {
        DeviceEntity device = deviceRepository.findByPublicIdAndUserId(devicePublicId, userId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_FOUND_DEVICE));

        if (!device.hasActiveSession()) return;

        if (device.getSessionId().equals(currentSessionId)) {
            throw new CustomException(BaseResponseStatus.SELF_DEVICE_FORCE_LOGOUT);
        }

        String targetSessionId = device.getSessionId();
        tokenService.invalidateSession(userId, targetSessionId);
        device.deactivateSession();

        log.info("Force Logout - UserPK: {} | DevicePublicId: {} | TargetSessionId: {}",
                userId, devicePublicId, targetSessionId);
    }

    @Override
    @Transactional
    public void deleteDevice(Long userId, String devicePublicId) {
        DeviceEntity device = deviceRepository.findByPublicIdAndUserId(devicePublicId, userId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_FOUND_DEVICE));

        if (device.hasActiveSession()) {
            throw new CustomException(BaseResponseStatus.ACTIVE_DEVICE_CANNOT_DELETE);
        }

        deviceRepository.delete(device);

        log.info("Device Removed - UserPK: {} | DevicePublicId: {} | Type: {}",
                userId, devicePublicId, device.getDeviceType().name());
    }

    private void enforceMaxSessionPolicy(Long userId, String currentSessionId) {
        List<DeviceEntity> activeDevices =
                deviceRepository.findActiveDevicesByUserIdOldestFirst(userId);

        if (activeDevices.size() <= deviceProperties.getMaxSessionsPerUser()) {
            return;
        }

        int excessCount = activeDevices.size() - deviceProperties.getMaxSessionsPerUser();

        // 안심 기기 제외하고 오래된 순으로 퇴출 대상 선정
        List<DeviceEntity> evictionTargets = activeDevices.stream()
                .filter(d -> !d.getSessionId().equals(currentSessionId))
                .filter(d -> !d.isTrusted())
                .limit(excessCount)
                .toList();

        for (DeviceEntity target : evictionTargets) {
            tokenService.invalidateSession(userId, target.getSessionId());
            target.deactivateSession();

            log.info("Max Session Policy - Evicted | UserPK: {} | DeviceId: {} | SessionId: {}",
                    userId, target.getId(), target.getSessionId());
        }

        // 퇴출할 일반 기기가 부족한 경우 (안심 기기가 너무 많을 때) — 허용
        if (evictionTargets.size() < excessCount) {
            log.info("Max Session Policy - Excess sessions allowed due to trusted devices | UserPK: {} | ActiveSessions: {} | Max: {}",
                    userId, activeDevices.size(), deviceProperties.getMaxSessionsPerUser());
        }
    }

    private DeviceEntity createNewDevice(Long userId, DeviceInfo deviceInfo) {
        return DeviceEntity.builder()
                .userId(userId)
                .deviceFingerprint(deviceInfo.getDeviceFingerprint())
                .deviceType(deviceInfo.getDeviceType())
                .osName(deviceInfo.getOsName())
                .browserName(deviceInfo.getBrowserName())
                .lastLoginIp(deviceInfo.getLoginIp())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

}