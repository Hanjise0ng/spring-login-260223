package com.han.back.global.security.filter;

import com.han.back.global.util.SecurityPathConst;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("LoginFilter")
class LoginFilterTest {

    private static final byte[] LOGIN_BODY =
            "{\"loginId\":\"testuser\",\"password\":\"Test1234!\"}".getBytes(StandardCharsets.UTF_8);

    private final AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
    private final LoginFilter loginFilter = new LoginFilter(
            authenticationManager,
            new ObjectMapper(),
            mock(AuthenticationSuccessHandler.class),
            mock(AuthenticationFailureHandler.class));

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    private static MockHttpServletRequest loginRequest(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, SecurityPathConst.LOGIN_PATH);
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.setContent(LOGIN_BODY);
        return request;
    }

    @Test
    @DisplayName("POST 로그인 요청은 AuthenticationManager에 인증을 위임한다")
    void post_delegatesToAuthenticationManager() throws Exception {
        loginFilter.doFilter(loginRequest("POST"), new MockHttpServletResponse(), new MockFilterChain());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"GET", "PUT", "PATCH", "DELETE"})
    @DisplayName("POST가 아닌 로그인 경로 요청은 인증을 시도하지 않고 다음 필터로 넘긴다")
    void nonPost_skipsAuthentication(String method) throws Exception {
        MockFilterChain chain = new MockFilterChain();

        loginFilter.doFilter(loginRequest(method), new MockHttpServletResponse(), chain);

        verifyNoInteractions(authenticationManager);
        assertThat(chain.getRequest()).isNotNull();
    }

}