package com.han.back.domain.device.dto.response;

import com.han.back.domain.device.entity.DeviceType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceReissueResponseDto {

    private final String sessionId;
    private final DeviceType deviceType;

    public static DeviceReissueResponseDto of(String sessionId, DeviceType deviceType) {
        return new DeviceReissueResponseDto(sessionId, deviceType);
    }
}