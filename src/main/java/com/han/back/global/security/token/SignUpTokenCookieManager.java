package com.han.back.global.security.token;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class SignUpTokenCookieManager {

    public void write(HttpServletResponse response, String token) {
        CookieUtil.addSecureCookie(response,
                OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME,
                token,
                OAuth2Const.SOCIAL_SIGN_UP_TOKEN_TTL,
                OAuth2Const.SOCIAL_SIGNUP_COOKIE_PATH);
    }

    public Optional<String> read(HttpServletRequest request) {
        return CookieUtil.getCookieValue(request, OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME);
    }

    public void clear(HttpServletResponse response) {
        CookieUtil.addSecureCookie(response,
                OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME,
                "",
                Duration.ZERO,
                OAuth2Const.SOCIAL_SIGNUP_COOKIE_PATH);
    }

}