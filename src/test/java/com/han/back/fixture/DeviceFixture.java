package com.han.back.fixture;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.entity.DeviceType;

import java.time.LocalDateTime;

public final class DeviceFixture {

    public static final String DEFAULT_WEB_FINGERPRINT = "fp-web-default-001";
    public static final String DEFAULT_APP_FINGERPRINT = "fp-app-ios-001";
    public static final String DEFAULT_SESSION_ID = "session-default-001";
    public static final LocalDateTime DEFAULT_LOGIN_AT = LocalDateTime.of(2026, 3, 30, 12, 0);

    private DeviceFixture() {}

    /** 활성 웹 데스크탑 디바이스 */
    public static DeviceEntity activeWebDevice(Long userId) {
        return builder(userId).build();
    }

    /** 비활성 디바이스 (로그아웃 상태, sessionId = null) */
    public static DeviceEntity inactiveDevice(Long userId) {
        return builder(userId)
                .sessionId(null)
                .fingerprint("fp-inactive-001")
                .build();
    }

    /** 활성 앱(iOS) 디바이스 */
    public static DeviceEntity activeAppDevice(Long userId) {
        return builder(userId)
                .fingerprint(DEFAULT_APP_FINGERPRINT)
                .deviceType(DeviceType.APP_IOS)
                .osName("iOS 17")
                .browserName("알 수 없음")
                .sessionId("session-app-001")
                .build();
    }

    public static DeviceInfo webDeviceInfo() {
        return DeviceInfo.of(
                DeviceType.WEB_DESKTOP,
                "Windows 11",
                "Chrome",
                DEFAULT_WEB_FINGERPRINT,
                "127.0.0.1"
        );
    }

    public static DeviceInfo appDeviceInfo() {
        return DeviceInfo.of(
                DeviceType.APP_IOS,
                "iOS 17",
                "알 수 없음",
                DEFAULT_APP_FINGERPRINT,
                "192.168.0.1"
        );
    }

    public static DeviceTestBuilder builder(Long userId) {
        return new DeviceTestBuilder(userId);
    }

    public static final class DeviceTestBuilder {

        private final Long userId;
        private String fingerprint = DEFAULT_WEB_FINGERPRINT;
        private DeviceType deviceType = DeviceType.WEB_DESKTOP;
        private String osName = "Windows 11";
        private String browserName = "Chrome";
        private String lastLoginIp = "127.0.0.1";
        private LocalDateTime loginAt = DEFAULT_LOGIN_AT;
        private String sessionId = DEFAULT_SESSION_ID;

        private DeviceTestBuilder(Long userId) {
            this.userId = userId;
        }

        public DeviceTestBuilder fingerprint(String fingerprint) {
            this.fingerprint = fingerprint;
            return this;
        }

        public DeviceTestBuilder deviceType(DeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public DeviceTestBuilder osName(String osName) {
            this.osName = osName;
            return this;
        }

        public DeviceTestBuilder browserName(String browserName) {
            this.browserName = browserName;
            return this;
        }

        public DeviceTestBuilder lastLoginIp(String ip) {
            this.lastLoginIp = ip;
            return this;
        }

        public DeviceTestBuilder loginAt(LocalDateTime loginAt) {
            this.loginAt = loginAt;
            return this;
        }

        public DeviceTestBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public DeviceEntity build() {
            return DeviceEntity.builder()
                    .userId(userId)
                    .deviceFingerprint(fingerprint)
                    .deviceType(deviceType)
                    .osName(osName)
                    .browserName(browserName)
                    .lastLoginIp(lastLoginIp)
                    .lastLoginAt(loginAt)
                    .sessionId(sessionId)
                    .build();
        }
    }

}