package com.han.back.domain.device.entity;

import com.han.back.global.entity.BaseTime;
import com.han.back.global.util.UuidUtil;
import jakarta.persistence.*;
import lombok.*;
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
                @Index(name = "idx_devices_user_last_login",
                        columnList = "user_id, last_login_at DESC"),
                @Index(name = "idx_devices_session_id",
                        columnList = "session_id")
        }
)
public class DeviceEntity extends BaseTime {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId = UuidUtil.generateString();

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

    /** 로그인 시 디바이스 정보 갱신 + 세션 활성화 */
    public void activateSession(String sessionId, DeviceType deviceType, String osName,
                                String browserName, String lastLoginIp) {
        this.sessionId = sessionId;
        this.deviceType = deviceType;
        this.osName = osName;
        this.browserName = browserName;
        this.lastLoginIp = lastLoginIp;
        this.lastLoginAt = LocalDateTime.now();
    }

    /** 토큰 재발급 시 세션 교체 (세션 롤링) */
    public void rotateSession(String newSessionId) {
        this.sessionId = newSessionId;
        this.lastLoginAt = LocalDateTime.now();
    }

    /** 로그아웃 시 세션 비활성화 */
    public void deactivateSession() {
        this.sessionId = null;
    }

    /** 현재 활성 세션이 존재하는지 확인 */
    public boolean hasActiveSession() {
        return this.sessionId != null;
    }

}