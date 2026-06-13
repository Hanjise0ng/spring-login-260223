package com.han.back.global.device;

import com.han.back.global.security.token.AuthConst;
import com.han.back.global.util.ClientIpResolver;
import com.han.back.global.util.CookieUtil;
import com.han.back.global.util.UuidUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DeviceRequestExtractor {

    public RawDeviceData extract(HttpServletRequest request) {
        boolean isNativeApp = AuthConst.CLIENT_TYPE_APP.equalsIgnoreCase(
                request.getHeader(AuthConst.HEADER_CLIENT_TYPE));

        return RawDeviceData.of(
                isNativeApp,
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(AuthConst.HEADER_DEVICE_OS),
                extractFingerprint(request, isNativeApp),
                ClientIpResolver.resolve(request)
        );
    }

    private String extractFingerprint(HttpServletRequest request, boolean isNativeApp) {
        if (isNativeApp) {
            String deviceId = request.getHeader(AuthConst.HEADER_DEVICE_ID);
            if (StringUtils.hasText(deviceId)) {
                return deviceId;
            }
        }
        return CookieUtil.getCookieValue(request, AuthConst.COOKIE_DEVICE_ID_NAME)
                .orElse(UuidUtil.generateString());
    }

}