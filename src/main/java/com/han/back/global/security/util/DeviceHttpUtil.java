package com.han.back.global.security.util;

import jakarta.servlet.http.HttpServletResponse;

public final class DeviceHttpUtil {

    private DeviceHttpUtil() {}

    public static void setDeviceIdCookie(HttpServletResponse response, String deviceFingerprint) {
        CookieUtil.addSecureCookie(
                response,
                AuthConst.COOKIE_DEVICE_ID_NAME,
                deviceFingerprint,
                AuthConst.COOKIE_DEVICE_ID_MAX_AGE
        );
    }

}