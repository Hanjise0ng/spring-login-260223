package com.han.back.global.device;

import com.han.back.global.security.token.AuthConst;
import com.han.back.global.util.CookieUtil;
import com.han.back.global.util.UuidUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * HTTP 요청에서 디바이스 관련 원시 데이터 추출
 *
 * <p>추출한 원시 데이터를 {@link RawDeviceData}로 포장하여 도메인 레이어에 전달</p>
 * @see RawDeviceData
 */
@Component
public class DeviceRequestExtractor {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    /**
     * HTTP 요청에서 디바이스 관련 원시 데이터를 추출
     *
     * @param request HTTP 요청
     * @return 도메인 타입을 포함하지 않는 원시 데이터
     */
    public RawDeviceData extract(HttpServletRequest request) {
        boolean isNativeApp = AuthConst.CLIENT_TYPE_APP.equalsIgnoreCase(
                request.getHeader(AuthConst.HEADER_CLIENT_TYPE));

        return RawDeviceData.of(
                isNativeApp,
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(AuthConst.HEADER_DEVICE_OS),
                extractFingerprint(request, isNativeApp),
                extractClientIp(request)
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

    private String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }

        ip = request.getHeader(HEADER_X_REAL_IP);
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }

}