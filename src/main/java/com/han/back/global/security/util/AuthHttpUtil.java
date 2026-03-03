package com.han.back.global.security.util;

import com.han.back.global.security.dto.AuthTokenDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

public class AuthHttpUtil {

    private AuthHttpUtil() {}

    public static String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public static String extractRefreshToken(HttpServletRequest request) {
        String headerToken = request.getHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME);
        if (StringUtils.hasText(headerToken)) return headerToken;
        return CookieUtil.getCookieValue(request, AuthConst.COOKIE_REFRESH_TOKEN_NAME).orElse(null);
    }

    public static void setTokenResponse(HttpServletRequest request, HttpServletResponse response, AuthTokenDto newTokens) {
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + newTokens.getAccessToken());

        String clientType = request.getHeader("X-Client-Type");
        if ("APP".equalsIgnoreCase(clientType)) {
            response.setHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME, newTokens.getRefreshToken());
        } else {
            CookieUtil.addSecureCookie(response, AuthConst.COOKIE_REFRESH_TOKEN_NAME, newTokens.getRefreshToken(), AuthConst.COOKIE_REFRESH_EXPIRATION);
        }
    }

}