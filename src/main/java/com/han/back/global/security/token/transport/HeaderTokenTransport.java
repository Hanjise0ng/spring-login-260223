package com.han.back.global.security.token.transport;

import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.AuthConst;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

@Component
public class HeaderTokenTransport implements TokenTransport{

    @Override
    public void write(HttpServletResponse response, AuthToken tokens) {
        response.setHeader(HttpHeaders.AUTHORIZATION,
                AuthConst.BEARER_PREFIX + tokens.getAccessToken());
        response.setHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME,
                tokens.getRefreshToken());
    }

    @Override
    public void writeDeviceCookie(HttpServletResponse response, String deviceFingerprint) {
        // 앱은 X-Device-Id 헤더로 fingerprint를 전송하므로 쿠키 불필요
    }

    @Override
    public void clear(HttpServletResponse response) {
        // 클라이언트 자체 폐기
    }

}
