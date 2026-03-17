package com.han.back.global.security.util;

import com.han.back.global.security.dto.AuthTokenDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

import java.util.Optional;

public class AuthHttpUtil {

    private AuthHttpUtil() {}

    public static Optional<String> extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(AuthConst.BEARER_PREFIX)) {
            return Optional.of(bearerToken.substring(7));
        }
        return Optional.empty();
    }

    public static Optional<String> extractRefreshToken(HttpServletRequest request) {
        String headerToken = request.getHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME);
        if (StringUtils.hasText(headerToken)) {
            return Optional.of(headerToken);
        }
        return CookieUtil.getCookieValue(request, AuthConst.COOKIE_REFRESH_TOKEN_NAME);
    }

    public static void setTokenResponse(HttpServletRequest request, HttpServletResponse response, AuthTokenDto newTokens) {
        response.setHeader(HttpHeaders.AUTHORIZATION, AuthConst.BEARER_PREFIX + newTokens.getAccessToken());

        ClientType clientType = ClientType.fromHeader(request.getHeader(AuthConst.HEADER_CLIENT_TYPE));
        if (clientType == ClientType.APP) {
            response.setHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME, newTokens.getRefreshToken());
        } else {
            CookieUtil.addSecureCookie(response, AuthConst.COOKIE_REFRESH_TOKEN_NAME,
                    newTokens.getRefreshToken(), AuthConst.COOKIE_REFRESH_EXPIRATION);
        }
    }

}