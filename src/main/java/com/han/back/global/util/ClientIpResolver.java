package com.han.back.global.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public final class ClientIpResolver {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String UNKNOWN = "unknown";

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (isValid(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader(HEADER_X_REAL_IP);
        if (isValid(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private static boolean isValid(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN.equalsIgnoreCase(ip);
    }

}