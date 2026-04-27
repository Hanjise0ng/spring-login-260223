package com.han.back.global.security.token.transport;

import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieTokenTransport implements TokenTransport {

    @Override
    public void write(HttpServletResponse response, AuthToken tokens) {
        response.setHeader(HttpHeaders.AUTHORIZATION,
                AuthConst.BEARER_PREFIX + tokens.getAccessToken());
        CookieUtil.addSecureCookie(response,
                AuthConst.COOKIE_REFRESH_TOKEN_NAME,
                tokens.getRefreshToken(),
                AuthConst.REFRESH_TOKEN_TTL);
    }

    @Override
    public void writeDeviceCookie(HttpServletResponse response, String deviceFingerprint) {
        CookieUtil.addSecureCookie(response,
                AuthConst.COOKIE_DEVICE_ID_NAME,
                deviceFingerprint,
                AuthConst.DEVICE_COOKIE_TTL);
    }

    @Override
    public void clear(HttpServletResponse response) {
        CookieUtil.addSecureCookie(response,
                AuthConst.COOKIE_REFRESH_TOKEN_NAME, "", Duration.ZERO);
    }

}