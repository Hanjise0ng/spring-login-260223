package com.han.back.domain.device.event;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.entity.DeviceType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NewDeviceLoginEvent {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final String deviceFingerprint;
    private final DeviceType deviceType;
    private final String osName;
    private final String loginIp;
    private final LocalDateTime loginAt;

    public static NewDeviceLoginEvent of(Long userId, String email, String nickname,
                                         DeviceInfo deviceInfo) {
        return new NewDeviceLoginEvent(
                userId,
                email,
                nickname,
                deviceInfo.getDeviceFingerprint(),
                deviceInfo.getDeviceType(),
                deviceInfo.getOsName(),
                deviceInfo.getLoginIp(),
                LocalDateTime.now()
        );
    }

}