package com.han.back.domain.device.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceRegistration {

    private final String sessionId;
    private final boolean newDevice;
    private final List<String> evictedSessionIds;

    public static DeviceRegistration of(String sessionId, boolean newDevice, List<String> evictedSessionIds) {
        return new DeviceRegistration(sessionId, newDevice, List.copyOf(evictedSessionIds));
    }

}