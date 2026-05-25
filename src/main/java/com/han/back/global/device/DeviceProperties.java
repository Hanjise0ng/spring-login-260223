package com.han.back.global.device;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.device")
public class DeviceProperties {

    private final int maxSessionsPerUser;
    private final int maxTrustedDevices;

    public DeviceProperties(int maxSessionsPerUser, int maxTrustedDevices) {
        this.maxSessionsPerUser = maxSessionsPerUser;
        this.maxTrustedDevices = maxTrustedDevices;
    }

    public int getMaxSessionsPerUser() {
        return maxSessionsPerUser;
    }

    public int getMaxTrustedDevices() {
        return maxTrustedDevices;
    }
}