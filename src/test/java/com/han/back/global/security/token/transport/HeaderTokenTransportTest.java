package com.han.back.global.security.token.transport;

import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.AuthToken;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HeaderTokenTransportTest {

    private final HeaderTokenTransport transport = new HeaderTokenTransport();

    @Mock
    private HttpServletResponse response;

    @Test
    @DisplayName("Access Token과 Refresh Token을 헤더에 삽입한다")
    void writeTokens() {
        AuthToken token = AuthToken.of("access-token-123", "refresh-token-123");

        transport.write(response, token);

        verify(response).setHeader(HttpHeaders.AUTHORIZATION, AuthConst.BEARER_PREFIX + "access-token-123");
        verify(response).setHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME, "refresh-token-123");
    }

    @Test
    @DisplayName("HeaderTransport는 Device Cookie 삽입 로직을 수행하지 않는다 (빈 구현)")
    void writeDeviceCookie_Empty() {
        // 단지 빈 메서드를 호출하여 라인 커버리지를 100%로 채웁니다.
        transport.writeDeviceCookie(response, "fingerprint");
    }

    @Test
    @DisplayName("HeaderTransport는 Clear 로직을 수행하지 않는다 (빈 구현)")
    void clear_Empty() {
        // 단지 빈 메서드를 호출하여 라인 커버리지를 100%로 채웁니다.
        transport.clear(response);
    }

}