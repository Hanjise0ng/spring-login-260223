package com.han.back.global.security.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RawDeviceData {

    private final boolean app;
    private final String userAgent;
    private final String deviceOs;
    private final String fingerprint;
    private final String loginIp;

    public static RawDeviceData of(boolean app, String userAgent,
                                   String deviceOs, String fingerprint,
                                   String loginIp) {
        return new RawDeviceData(app, userAgent, deviceOs, fingerprint, loginIp);
    }

}