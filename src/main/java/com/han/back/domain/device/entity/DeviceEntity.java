package com.han.back.domain.device.entity;

import com.han.back.domain.user.entity.UserEntity;
import com.han.back.global.entity.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "devices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_devices_user_fingerprint",
                        columnNames = {"user_id", "device_fingerprint"}
                )
        },
        indexes = {
                @Index(name = "idx_devices_user_id", columnList = "user_id"),
                @Index(name = "idx_devices_session_id", columnList = "session_id")
        }
)
public class DeviceEntity extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_devices_user_id"))
    private UserEntity user;

    @Column(name = "device_fingerprint", nullable = false, length = 64)
    private String deviceFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "os_name", nullable = false, length = 50)
    private String osName;

    @Column(name = "browser_name", nullable = false, length = 50)
    private String browserName;

    @Column(name = "last_login_ip", nullable = false, length = 45)
    private String lastLoginIp;

    @Column(name = "last_login_at", nullable = false)
    private LocalDateTime lastLoginAt;

    @Column(name = "session_id", length = 36)
    private String sessionId;

    // 로그인 시 디바이스 정보 갱신 + 세션 활성화
    public void activateSession(String sessionId, DeviceType deviceType, String osName,
                                String browserName, String lastLoginIp) {
        this.sessionId = sessionId;
        this.deviceType = deviceType;
        this.osName = osName;
        this.browserName = browserName;
        this.lastLoginIp = lastLoginIp;
        this.lastLoginAt = LocalDateTime.now();
    }

    // 토큰 재발급 시 세션 교체
    public void rotateSession(String newSessionId) {
        this.sessionId = newSessionId;
        this.lastLoginAt = LocalDateTime.now();
    }

    // 로그아웃 시 세션 비활성화
    public void deactivateSession() {
        this.sessionId = null;
    }

    // 현재 활성 세션이 존재하는지 확인
    public boolean hasActiveSession() {
        return this.sessionId != null;
    }

}