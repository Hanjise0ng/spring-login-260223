package com.han.back.fixture;

import com.han.back.domain.device.dto.DeviceInfoDto;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.entity.DeviceType;
import com.han.back.domain.user.entity.UserEntity;

import java.time.LocalDateTime;

public final class DeviceFixture {

    private DeviceFixture() {}

    // 활성 웹 데스크탑 디바이스
    public static DeviceEntity activeWebDevice(UserEntity user) {
        return builder(user).build();
    }

    // 비활성 디바이스 (로그아웃 상태, sessionId = null)
    public static DeviceEntity inactiveDevice(UserEntity user) {
        return builder(user)
                .sessionId(null)
                .fingerprint("fp-inactive-001")
                .build();
    }

    // 활성 앱(iOS) 디바이스
    public static DeviceEntity activeAppDevice(UserEntity user) {
        return builder(user)
                .fingerprint("fp-app-ios-001")
                .deviceType(DeviceType.APP_IOS)
                .osName("iOS 17")
                .browserName("알 수 없음")
                .sessionId("session-app-001")
                .build();
    }

    public static DeviceInfoDto webDeviceInfo() {
        return DeviceInfoDto.of(
                DeviceType.WEB_DESKTOP,
                "Windows 11",
                "Chrome",
                "fp-web-default-001",
                "127.0.0.1"
        );
    }

    public static DeviceInfoDto appDeviceInfo() {
        return DeviceInfoDto.of(
                DeviceType.APP_IOS,
                "iOS 17",
                "알 수 없음",
                "fp-app-ios-001",
                "192.168.0.1"
        );
    }

    public static DeviceTestBuilder builder(UserEntity user) {
        return new DeviceTestBuilder(user);
    }

    public static final class DeviceTestBuilder {

        private final UserEntity user;
        private String fingerprint = "fp-web-default-001";
        private DeviceType deviceType = DeviceType.WEB_DESKTOP;
        private String osName = "Windows 11";
        private String browserName = "Chrome";
        private String lastLoginIp = "127.0.0.1";
        private LocalDateTime loginAt = LocalDateTime.of(2026, 3, 30, 12, 0);
        private String sessionId = "session-default-001";

        private DeviceTestBuilder(UserEntity user) {
            this.user = user;
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
                    .user(user)
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