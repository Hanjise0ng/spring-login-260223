package com.han.back.global.device;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "app.device")
public class DeviceProperties {

    private final int maxSessionsPerUser;
    private final int maxTrustedDevices;

    public DeviceProperties(int maxSessionsPerUser, int maxTrustedDevices) {
        this.maxSessionsPerUser = maxSessionsPerUser;
        this.maxTrustedDevices = maxTrustedDevices;
    }

}