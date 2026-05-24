package com.han.back.domain.device.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceRegistration {

    private final String sessionId;

    public static DeviceRegistration of(String sessionId) {
        return new DeviceRegistration(sessionId);
    }

}