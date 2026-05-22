package com.han.back.global.device;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.service.DeviceInfoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceInfoResolver {

    private final DeviceRequestExtractor deviceRequestExtractor;
    private final DeviceInfoService deviceInfoService;

    public DeviceInfo resolve(HttpServletRequest request) {
        return deviceInfoService.resolveDeviceInfo(deviceRequestExtractor.extract(request));
    }

}