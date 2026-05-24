package com.han.back.global.device;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.mapper.DeviceInfoMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeviceInfoProvider {

    private final DeviceRequestExtractor extractor;
    private final DeviceInfoMapper mapper;

    public DeviceInfo get(HttpServletRequest request) {
        return mapper.map(extractor.extract(request));
    }

}