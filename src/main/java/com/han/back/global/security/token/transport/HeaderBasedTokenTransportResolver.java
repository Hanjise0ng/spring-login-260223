package com.han.back.global.security.token.transport;

import com.han.back.global.security.token.AuthConst;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class HeaderBasedTokenTransportResolver implements TokenTransportResolver {

    private final TokenTransport cookieDelivery;
    private final TokenTransport headerDelivery;

    public HeaderBasedTokenTransportResolver(
            CookieTokenTransport cookieDelivery,
            HeaderTokenTransport headerDelivery) {
        this.cookieDelivery = cookieDelivery;
        this.headerDelivery = headerDelivery;
    }

    @Override
    public TokenTransport resolve(HttpServletRequest request) {
        String clientType = request.getHeader(AuthConst.HEADER_CLIENT_TYPE);
        return AuthConst.CLIENT_TYPE_APP.equalsIgnoreCase(clientType) ? headerDelivery : cookieDelivery;
    }

}