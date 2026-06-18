package com.han.back.global.security.token;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.security.oauth2.SignUpTokenCookieManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.Cookie;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SignUpTokenCookieManager")
class SignUpTokenCookieManagerTest {

    private final SignUpTokenCookieManager manager = new SignUpTokenCookieManager();

    @Test
    @DisplayName("write는 HttpOnly·지정 Path로 social_signup_token 쿠키를 심는다")
    void writeSetsHttpOnlyCookieWithPath() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        manager.write(response, "temp-token-value");

        String setCookie = String.join("\n", response.getHeaders("Set-Cookie"));
        assertThat(setCookie).contains(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME + "=temp-token-value");
        assertThat(setCookie).contains("Path=" + OAuth2Const.SOCIAL_SIGNUP_COOKIE_PATH);
        assertThat(setCookie).contains("HttpOnly");
    }

    @Test
    @DisplayName("read는 요청 쿠키에서 social_signup_token 값을 꺼낸다")
    void readExtractsToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME, "temp-token-value"));

        Optional<String> result = manager.read(request);

        assertThat(result).contains("temp-token-value");
    }

    @Test
    @DisplayName("read는 쿠키가 없으면 빈 Optional을 반환한다")
    void readReturnsEmptyWhenAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        Optional<String> result = manager.read(request);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("clear는 발급과 동일 Path로 maxAge=0 쿠키를 심어 만료시킨다")
    void clearExpiresCookieWithSamePath() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        manager.clear(response);

        String setCookie = String.join("\n", response.getHeaders("Set-Cookie"));
        assertThat(setCookie).contains(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME + "=");
        assertThat(setCookie).contains("Path=" + OAuth2Const.SOCIAL_SIGNUP_COOKIE_PATH);
        assertThat(setCookie).contains("Max-Age=0");
    }

}