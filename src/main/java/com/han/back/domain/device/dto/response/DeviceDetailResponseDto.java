package com.han.back.domain.device.dto.response;

import com.han.back.domain.device.entity.DeviceEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceDetailResponseDto {

    private final Long deviceId;
    private final String deviceType;
    private final String deviceTypeName;
    private final String osName;
    private final String browserName;
    private final String lastLoginIp;
    private final LocalDateTime lastLoginAt;
    private final LocalDateTime firstLoginAt;
    private final boolean currentDevice;
    private final boolean activeSession;

    public static DeviceDetailResponseDto from(DeviceEntity device, String currentSessionId) {
        boolean isActive = device.hasActiveSession();
        boolean isCurrent = isActive && device.getSessionId().equals(currentSessionId);

        return DeviceDetailResponseDto.builder()
                .deviceId(device.getId())
                .deviceType(device.getDeviceType().name())
                .deviceTypeName(device.getDeviceType().getDisplayName())
                .osName(device.getOsName())
                .browserName(device.getBrowserName())
                .lastLoginIp(device.getLastLoginIp())
                .lastLoginAt(device.getLastLoginAt())
                .firstLoginAt(device.getCreatedAt())
                .currentDevice(isCurrent)
                .activeSession(isActive)
                .build();
    }

}