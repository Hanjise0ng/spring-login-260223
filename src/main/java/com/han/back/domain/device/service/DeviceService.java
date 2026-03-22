package com.han.back.domain.device.service;

import com.han.back.domain.device.dto.DeviceInfoDto;
import com.han.back.domain.device.dto.response.DeviceSignInResponseDto;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;

import java.util.List;

public interface DeviceService {

    /**
     * 로그인 시 디바이스 등록/갱신 + 세션 생성 + 최대 세션 정책 적용.
     *
     * @param userId     로그인 사용자 PK
     * @param deviceInfo User-Agent 파싱 결과 (웹 레이어에서 추출된 디바이스 정보)
     * @return sessionId + deviceFingerprint (토큰 발급 및 쿠키 설정에 사용)
     */
    DeviceSignInResponseDto registerLoginDevice(Long userId, DeviceInfoDto deviceInfo);

    /**
     * 세션 비활성화 — 로그아웃 시 DeviceEntity의 sessionId를 제거.
     * TokenService.invalidateSession()과 별도로 호출되어야 한다.
     */
    void deactivateSession(Long userId, String sessionId);

    /**
     * 사용자의 전체 디바이스 목록 조회.
     *
     * @param userId             조회 대상 사용자 PK
     * @param currentSessionId   현재 요청자의 세션 ID (currentDevice 판별용)
     * @return 디바이스 목록 (최근 로그인 순)
     */
    List<DeviceDetailResponseDto> getDevices(Long userId, String currentSessionId);

    /**
     * 특정 디바이스 강제 로그아웃.
     * 대상 디바이스의 세션을 무효화하고 DB에서 비활성화한다.
     *
     * @param userId           요청 사용자 PK (소유권 검증)
     * @param deviceId         대상 디바이스 PK
     * @param currentSessionId 현재 요청자의 세션 ID (자기 자신 강제 로그아웃 방지)
     */
    void forceLogoutDevice(Long userId, Long deviceId, String currentSessionId);

}