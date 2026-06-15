package com.han.back.domain.device.service.implement;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.exception.DeviceResponseStatus;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.global.device.DeviceProperties;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        List<String> evictedSessionIds = evictExcessSessions(userId, sessionId);

        log.info("Device Registered - UserPK: {} | DeviceId: {} | Type: {} | SessionId: {} | IP: {} | NewDevice: {}",
                device.getUserId(), device.getId(), deviceInfo.getDeviceType().name(),
                sessionId, deviceInfo.getLoginIp(), isNewDevice);

        return DeviceRegistration.of(sessionId, isNewDevice, evictedSessionIds);
    }

    @Override
    @Transactional
    public void trustDevice(Long userId, String devicePublicId) {
        DeviceEntity device = deviceRepository.findByPublicIdAndUserId(devicePublicId, userId)
                .orElseThrow(() -> new CustomException(DeviceResponseStatus.DEVICE_NOT_FOUND));

        if (device.isTrusted()) return;

        int trustedCount = deviceRepository.countTrustedDevicesByUserId(userId);
        if (trustedCount >= deviceProperties.getMaxTrustedDevices()) {
            throw new CustomException(DeviceResponseStatus.DEVICE_TRUSTED_LIMIT_EXCEEDED);
        }

        device.markAsTrusted();

        log.info("Device Trusted - UserPK: {} | DevicePublicId: {}", userId, devicePublicId);
    }

    @Override
    @Transactional
    public void untrustDevice(Long userId, String devicePublicId) {
        DeviceEntity device = deviceRepository.findByPublicIdAndUserId(devicePublicId, userId)
                .orElseThrow(() -> new CustomException(DeviceResponseStatus.DEVICE_NOT_FOUND));

        if (!device.isTrusted()) return;

        device.unmarkTrusted();

        log.info("Device Untrusted - UserPK: {} | DevicePublicId: {}", userId, devicePublicId);
    }

    @Override
    @Transactional
    public String rotateDeviceSession(Long userId, String oldSessionId) {
        DeviceEntity device = deviceRepository.findByUserIdAndSessionId(userId, oldSessionId)
                .orElseThrow(() -> new CustomAuthenticationException(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL));

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
                .orElseThrow(() -> new CustomException(DeviceResponseStatus.DEVICE_NOT_FOUND));

        if (!device.hasActiveSession()) return;

        if (device.getSessionId().equals(currentSessionId)) {
            throw new CustomException(DeviceResponseStatus.DEVICE_SELF_LOGOUT_FORBIDDEN);
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
                .orElseThrow(() -> new CustomException(DeviceResponseStatus.DEVICE_NOT_FOUND));

        if (device.hasActiveSession()) {
            throw new CustomException(DeviceResponseStatus.DEVICE_ACTIVE_DELETE_FORBIDDEN);
        }

        deviceRepository.delete(device);

        log.info("Device Removed - UserPK: {} | DevicePublicId: {} | Type: {}",
                userId, devicePublicId, device.getDeviceType().name());
    }

    private List<String> evictExcessSessions(Long userId, String currentSessionId) {
        List<DeviceEntity> activeDevices =
                deviceRepository.findActiveDevicesByUserIdOldestFirst(userId);

        if (activeDevices.size() <= deviceProperties.getMaxSessionsPerUser()) {
            return List.of();
        }

        int excessCount = activeDevices.size() - deviceProperties.getMaxSessionsPerUser();

        List<DeviceEntity> evictionTargets = activeDevices.stream()
                .filter(d -> !d.getSessionId().equals(currentSessionId))
                .filter(d -> !d.isTrusted())
                .limit(excessCount)
                .toList();

        List<String> evictedSessionIds = new ArrayList<>();
        for (DeviceEntity target : evictionTargets) {
            evictedSessionIds.add(target.getSessionId());
            target.deactivateSession();

            log.info("Max Session Policy - Evicted | UserPK: {} | DeviceId: {} | SessionId: {}",
                    userId, target.getId(), target.getSessionId());
        }

        if (evictionTargets.size() < excessCount) {
            log.info("Max Session Policy - Excess sessions allowed due to trusted devices | UserPK: {} | ActiveSessions: {} | Max: {}",
                    userId, activeDevices.size(), deviceProperties.getMaxSessionsPerUser());
        }

        return evictedSessionIds;
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