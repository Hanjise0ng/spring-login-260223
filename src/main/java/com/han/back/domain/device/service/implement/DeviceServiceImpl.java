package com.han.back.domain.device.service.implement;

import com.han.back.domain.device.dto.DeviceInfoDto;
import com.han.back.domain.device.dto.response.DeviceSignInResponseDto;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.entity.DeviceConst;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.dto.BaseResponseStatus;
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

        // 디바이스 정보 갱신 + 세션 활성화
        device.activateSession(
                sessionId,
                deviceInfo.getDeviceType(),
                deviceInfo.getOsName(),
                deviceInfo.getBrowserName(),
                deviceInfo.getLoginIp()
        );

        deviceRepository.save(device);

        // 최대 세션 정책 적용 (현재 세션 제외)
        enforceMaxSessionPolicy(userId, sessionId);

        log.info("Device Registered - UserPK: {} | DeviceId: {} | Type: {} | SessionId: {} | IP: {}",
                userId, device.getId(), deviceInfo.getDeviceType().name(), sessionId, deviceInfo.getLoginIp());

        return DeviceSignInResponseDto.of(sessionId, deviceInfo.getDeviceFingerprint());
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
    public void forceLogoutDevice(Long userId, Long deviceId, String currentSessionId) {
        DeviceEntity device = deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.NOT_FOUND_DEVICE));

        // 이미 로그아웃 상태 → 의도한 결과 달성, DELETE 멱등성 원칙에 따라 성공 처리
        if (!device.hasActiveSession()) {
            return;
        }

        // 현재 접속 중인 기기는 강제 로그아웃 불가 (일반 로그아웃 사용)
        if (device.getSessionId().equals(currentSessionId)) {
            throw new CustomException(BaseResponseStatus.SELF_DEVICE_FORCE_LOGOUT);
        }

        String targetSessionId = device.getSessionId();

        // Redis: 세션 블랙리스트 등록 + RT 삭제
        tokenService.invalidateSession(userId, targetSessionId);

        // DB: 디바이스 세션 비활성화
        device.deactivateSession();

        log.info("Force Logout - UserPK: {} | DeviceId: {} | TargetSessionId: {}",
                userId, deviceId, targetSessionId);
    }

    /**
     * 최대 세션 수 초과 시 가장 오래된 세션을 자동 무효화한다.
     *
     * @param userId           사용자 PK
     * @param currentSessionId 방금 생성된 세션 (무효화 대상에서 제외)
     */
    private void enforceMaxSessionPolicy(Long userId, String currentSessionId) {
        List<DeviceEntity> activeDevices = deviceRepository.findActiveDevicesByUserIdOldestFirst(userId);

        // 최대 세션 수 이하면 조치 불필요
        if (activeDevices.size() <= DeviceConst.MAX_SESSIONS_PER_USER) {
            return;
        }

        // 초과분만큼 가장 오래된 세션부터 무효화
        int excessCount = activeDevices.size() - DeviceConst.MAX_SESSIONS_PER_USER;

        for (int i = 0; i < excessCount; i++) {
            DeviceEntity oldestDevice = activeDevices.get(i);

            // 방금 생성된 세션은 건너뛰기
            if (oldestDevice.getSessionId().equals(currentSessionId)) {
                continue;
            }

            tokenService.invalidateSession(userId, oldestDevice.getSessionId());
            oldestDevice.deactivateSession();

            log.info("Max Session Policy - Evicted oldest session | UserPK: {} | DeviceId: {} | EvictedSessionId: {}",
                    userId, oldestDevice.getId(), oldestDevice.getSessionId());
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