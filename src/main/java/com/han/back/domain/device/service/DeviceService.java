package com.han.back.domain.device.service;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;

import java.util.List;

public interface DeviceService {

    DeviceRegistration registerLoginDevice(Long userId, DeviceInfo deviceInfo);

    String rotateDeviceSession(Long userId, String oldSessionId);

    void deactivateSession(Long userId, String sessionId);

    List<DeviceDetailResponseDto> getDevices(Long userId, String currentSessionId);

    void forceLogoutDevice(Long userId, String devicePublicId, String currentSessionId);

    void deleteDevice(Long userId, String devicePublicId);

}