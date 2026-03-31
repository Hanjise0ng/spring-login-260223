package com.han.back.domain.device.repository;

import com.han.back.domain.device.entity.DeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<DeviceEntity, Long> {

    // 사용자 + fingerprint 조합으로 기기 조회 (로그인 시 기존 기기 식별)
    Optional<DeviceEntity> findByUserIdAndDeviceFingerprint(Long userId, String deviceFingerprint);

    // 사용자의 특정 디바이스 조회 (소유권 검증 포함)
    Optional<DeviceEntity> findByPublicIdAndUserId(String publicId, Long userId);

    // 특정 세션 ID를 가진 디바이스 조회 (재발급 시 기기 식별용)
    Optional<DeviceEntity> findByUserIdAndSessionId(Long userId, String sessionId);

    // 사용자의 전체 디바이스 목록 — 최근 로그인 순 정렬
    @Query("SELECT d FROM DeviceEntity d WHERE d.user.id = :userId ORDER BY d.lastLoginAt DESC")
    List<DeviceEntity> findAllByUserIdOrderByLastLoginAtDesc(@Param("userId") Long userId);

    // 사용자의 활성 세션 디바이스 목록 — 오래된 순 정렬 (최대 세션 정책용)
    @Query("SELECT d FROM DeviceEntity d " +
            "WHERE d.user.id = :userId AND d.sessionId IS NOT NULL " +
            "ORDER BY d.lastLoginAt ASC")
    List<DeviceEntity> findActiveDevicesByUserIdOldestFirst(@Param("userId") Long userId);

    // 특정 세션 ID로 디바이스 세션 비활성화 — LogoutHandler, 강제 로그아웃에서 사용
    @Modifying(clearAutomatically = true)
    @Query("UPDATE DeviceEntity d SET d.sessionId = NULL WHERE d.user.id = :userId AND d.sessionId = :sessionId")
    int deactivateSessionByUserIdAndSessionId(@Param("userId") Long userId, @Param("sessionId") String sessionId);

}