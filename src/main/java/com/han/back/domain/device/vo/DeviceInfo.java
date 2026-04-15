package com.han.back.domain.device.vo;

import com.han.back.domain.device.entity.DeviceType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceInfo {

    private final DeviceType deviceType;
    private final String osName;
    private final String browserName;
    private final String deviceFingerprint;
    private final String loginIp;

    public static DeviceInfo of(DeviceType deviceType, String osName, String browserName,
                                String deviceFingerprint, String loginIp) {
        return DeviceInfo.builder()
                .deviceType(deviceType)
                .osName(osName)
                .browserName(browserName)
                .deviceFingerprint(deviceFingerprint)
                .loginIp(loginIp)
                .build();
    }

}