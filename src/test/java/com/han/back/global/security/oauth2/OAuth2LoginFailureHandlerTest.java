package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2LoginFailureHandler")
class OAuth2LoginFailureHandlerTest {

    private static final String FRONT_BASE_URL = "https://service.com";

    private OAuth2LoginFailureHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2LoginFailureHandler();
        ReflectionTestUtils.setField(handler, "frontBaseUrl", FRONT_BASE_URL);
    }

    private MockHttpServletResponse handle(AuthenticationException exception) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationFailure(new MockHttpServletRequest(), response, exception);
        return response;
    }

    @Test
    @DisplayName("실패 시 프론트 로그인 페이지로 고정 에러코드를 붙여 리다이렉트한다")
    void redirectsToFrontWithFixedErrorCode() throws Exception {
        MockHttpServletResponse response = handle(new OAuth2AuthenticationException("invalid_token"));

        String redirected = response.getRedirectedUrl();
        assertThat(redirected).startsWith(FRONT_BASE_URL + OAuth2Const.FRONT_LOGIN_ERROR_PATH);
        assertThat(redirected).contains("error=" + OAuth2Const.ERROR_SOCIAL_LOGIN_FAILED);
    }

    @Test
    @DisplayName("내부 예외 메시지를 응답에 노출하지 않는다 (정보 노출 방어)")
    void doesNotLeakExceptionMessage() throws Exception {
        String sensitive = "token signature mismatch for client secret abc123";

        MockHttpServletResponse response = handle(new OAuth2AuthenticationException(sensitive));

        assertThat(response.getRedirectedUrl()).doesNotContain(sensitive);
        assertThat(response.getRedirectedUrl()).doesNotContain("abc123");
    }

    @Test
    @DisplayName("예외 종류와 무관하게 동일한 실패 응답을 내려준다")
    void uniformResponseRegardlessOfExceptionType() throws Exception {
        MockHttpServletResponse first = handle(new OAuth2AuthenticationException("access_denied"));
        MockHttpServletResponse second = handle(new OAuth2AuthenticationException("server_error"));

        assertThat(first.getRedirectedUrl()).isEqualTo(second.getRedirectedUrl());
    }

}