package com.han.back.global.security.token.transport;

import com.han.back.global.security.token.AuthConst;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HeaderBasedTokenTransportResolverTest {

    @Mock private CookieTokenTransport cookieDelivery;
    @Mock private HeaderTokenTransport headerDelivery;
    @Mock private HttpServletRequest request;

    private HeaderBasedTokenTransportResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new HeaderBasedTokenTransportResolver(cookieDelivery, headerDelivery);
    }

    @Test
    @DisplayName("클라이언트 타입이 APP이면 Header 방식을 선택한다")
    void resolveAppClient() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn(AuthConst.CLIENT_TYPE_APP);

        // when
        TokenTransport transport = resolver.resolve(request);

        // then
        assertThat(transport).isSameAs(headerDelivery);
    }

    @Test
    @DisplayName("클라이언트 타입이 WEB이거나 헤더가 없으면 Cookie 방식을 선택한다")
    void resolveWebOrNullClient() {
        // given
        given(request.getHeader(AuthConst.HEADER_CLIENT_TYPE)).willReturn(null);

        // when
        TokenTransport transport = resolver.resolve(request);

        // then
        assertThat(transport).isSameAs(cookieDelivery);
    }

}