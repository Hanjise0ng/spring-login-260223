package com.han.back.domain.auth.attempt.entity;

import com.han.back.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "login_attempts",
        indexes = {
                @Index(name = "idx_login_attempts_user_created",
                        columnList = "user_id, created_at DESC")
        }
)
public class LoginAttemptEntity extends BaseTime {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "ip", nullable = false, length = 45)
    private String ip;

    @Column(name = "device_fingerprint", length = 64)
    private String deviceFingerprint;

    public static LoginAttemptEntity success(Long userId, String ip, String deviceFingerprint) {
        return LoginAttemptEntity.builder()
                .userId(userId)
                .success(true)
                .ip(ip)
                .deviceFingerprint(deviceFingerprint)
                .build();
    }

    public static LoginAttemptEntity failure(Long userId, String ip, String deviceFingerprint) {
        return LoginAttemptEntity.builder()
                .userId(userId)
                .success(false)
                .ip(ip)
                .deviceFingerprint(deviceFingerprint)
                .build();
    }

}