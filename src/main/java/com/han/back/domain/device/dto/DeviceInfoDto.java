package com.han.back.domain.device.dto;

import com.han.back.domain.device.entity.DeviceType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * User-Agent 파싱 + 헤더 분석 결과를 담는 불변 DTO.
 * UserAgentUtil → DeviceService로 전달되는 내부 전용 객체.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceInfoDto {

    private final DeviceType deviceType;
    private final String osName;
    private final String browserName;
    private final String deviceFingerprint;
    private final String loginIp;

    public static DeviceInfoDto of(DeviceType deviceType, String osName, String browserName,
                                   String deviceFingerprint, String loginIp) {
        return DeviceInfoDto.builder()
                .deviceType(deviceType)
                .osName(osName)
                .browserName(browserName)
                .deviceFingerprint(deviceFingerprint)
                .loginIp(loginIp)
                .build();
    }

}