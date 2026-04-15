package com.han.back.domain.device.service;

import com.han.back.domain.device.vo.DeviceInfo;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.dto.response.DeviceReissueResponseDto;
import com.han.back.domain.device.dto.response.DeviceSignInResponseDto;

import java.util.List;

public interface DeviceService {

    /**
     * 로그인 시 디바이스 등록/갱신 + 세션 생성 + 최대 세션 정책 적용.
     *
     * @param userId     로그인 사용자 PK
     * @param deviceInfo User-Agent 파싱 결과 (웹 레이어에서 추출된 디바이스 정보)
     * @return sessionId + deviceFingerprint (토큰 발급 및 쿠키 설정에 사용)
     */
    DeviceSignInResponseDto registerLoginDevice(Long userId, DeviceInfo deviceInfo);

    /**
     * 재발급 시 디바이스의 세션 ID를 교체하고 재발급 결과를 반환
     *
     * @param userId       로그인 사용자 PK
     * @param oldSessionId 해당 디바이스의 기존 sessionId
     * @return 새 sessionId + 디바이스 타입 (토큰 응답 방식 결정에 사용)
     */
    DeviceReissueResponseDto rotateDeviceSession(Long userId, String oldSessionId);

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
     * @param devicePublicId   대상 디바이스 PublicId
     * @param currentSessionId 현재 요청자의 세션 ID (자기 자신 강제 로그아웃 방지)
     */
    void forceLogoutDevice(Long userId, String devicePublicId, String currentSessionId);

    /**
     * 비활성 디바이스를 목록에서 제거한다.
     * 활성 세션이 남아있는 디바이스는 삭제 불가 — 강제 로그아웃 후 삭제해야 한다.
     *
     * @param userId           요청 사용자 PK (소유권 검증)
     * @param devicePublicId   대상 디바이스 PublicId
     */
    void deleteDevice(Long userId, String devicePublicId);

}