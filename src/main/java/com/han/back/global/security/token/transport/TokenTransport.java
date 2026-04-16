package com.han.back.global.security.token.transport;

import com.han.back.global.security.token.AuthToken;
import jakarta.servlet.http.HttpServletResponse;

public interface TokenTransport {

    void write(HttpServletResponse response, AuthToken tokens);

    void writeDeviceCookie(HttpServletResponse response, String deviceFingerprint);

    void clear(HttpServletResponse response);

}