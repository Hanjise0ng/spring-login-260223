package com.han.back.domain.device.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceSignInResponseDto {

    private final String sessionId;
    private final String deviceFingerprint;

    public static DeviceSignInResponseDto of(String sessionId, String deviceFingerprint) {
        return new DeviceSignInResponseDto(sessionId, deviceFingerprint);
    }

}