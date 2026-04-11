package com.han.back.domain.device.service.implement;

import com.han.back.domain.device.dto.DeviceInfoDto;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.dto.response.DeviceSignInResponseDto;
import com.han.back.domain.device.entity.DeviceConst;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    @Override
    @Transactional
    public DeviceSignInResponseDto registerLoginDevice(Long userId, DeviceInfoDto deviceInfo) {
        String sessionId = UuidUtil.generateString();

        DeviceEntity device = deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceInfo.getDeviceFingerprint())
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

        log.info("Device Registered - UserPK: {} | DeviceId: {} | Type: {} | SessionId: {} | IP: {}",
                userId, device.getId(), deviceInfo.getDeviceType().name(), sessionId, deviceInfo.getLoginIp());

        return DeviceSignInResponseDto.of(sessionId, deviceInfo.getDeviceFingerprint());
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
    public void deleteDevice(Long userId, String devicePublicId){
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
        List<DeviceEntity> activeDevices = deviceRepository.findActiveDevicesByUserIdOldestFirst(userId);

        if (activeDevices.size() <= DeviceConst.MAX_SESSIONS_PER_USER) {
            return;
        }

        int excessCount = activeDevices.size() - DeviceConst.MAX_SESSIONS_PER_USER;

        List<DeviceEntity> evictionTargets = activeDevices.stream()
                .filter(device -> !device.getSessionId().equals(currentSessionId))
                .limit(excessCount)
                .toList();

        for (DeviceEntity target : evictionTargets) {
            String evictedSessionId = target.getSessionId();

            tokenService.invalidateSession(userId, evictedSessionId);
            target.deactivateSession();

            log.info("Max Session Policy - Evicted oldest session | UserPK: {} | DeviceId: {} | EvictedSessionId: {}",
                    userId, target.getId(), evictedSessionId);
        }
    }

    private DeviceEntity createNewDevice(Long userId, DeviceInfoDto deviceInfo) {
        UserEntity user = userRepository.getReferenceById(userId);

        return DeviceEntity.builder()
                .user(user)
                .deviceFingerprint(deviceInfo.getDeviceFingerprint())
                .deviceType(deviceInfo.getDeviceType())
                .osName(deviceInfo.getOsName())
                .browserName(deviceInfo.getBrowserName())
                .lastLoginIp(deviceInfo.getLoginIp())
                .lastLoginAt(LocalDateTime.now())
                .build();
    }

}