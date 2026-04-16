package com.han.back.global.security.token;

import com.han.back.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
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

    public static AuthToken extractTokenPairLeniently(HttpServletRequest request) {
        String accessToken = extractAccessToken(request).orElse("");
        String refreshToken = extractRefreshToken(request).orElse("");
        return AuthToken.of(accessToken, refreshToken);
    }

}