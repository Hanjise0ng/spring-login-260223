package com.han.back.global.security.token.transport;

import jakarta.servlet.http.HttpServletRequest;

public interface TokenTransportResolver {

    TokenTransport resolve(HttpServletRequest request);

}