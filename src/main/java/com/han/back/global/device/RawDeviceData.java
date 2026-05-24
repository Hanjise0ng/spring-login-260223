package com.han.back.global.device;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RawDeviceData {

    private final boolean nativeApp;
    private final String userAgent;
    private final String deviceOs;
    private final String fingerprint;
    private final String loginIp;

    public static RawDeviceData of(boolean nativeApp, String userAgent,
                                   String deviceOs, String fingerprint,
                                   String loginIp) {
        return new RawDeviceData(nativeApp, userAgent, deviceOs, fingerprint, loginIp);
    }

}