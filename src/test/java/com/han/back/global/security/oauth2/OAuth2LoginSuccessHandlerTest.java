package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.fixture.DeviceFixture;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.util.SocialSignUpTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2LoginSuccessHandler")
class OAuth2LoginSuccessHandlerTest {

    @Mock private AuthService authService;
    @Mock private SocialSignUpTokenUtil socialSignUpTokenUtil;
    @Mock private DeviceInfoProvider deviceInfoProvider;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oAuth2User;
    @Mock private OAuth2UserInfo userInfo;

    @InjectMocks private OAuth2LoginSuccessHandler handler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private DeviceInfo deviceInfo;

    private static final String FRONT = "http://localhost:3000";
    private static final String RT = "refresh-token-value";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontBaseUrl", FRONT);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        deviceInfo = DeviceFixture.webDeviceInfo();
        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(Map.of(OAuth2Const.ATTR_USER_INFO, userInfo));
        given(deviceInfoProvider.get(request)).willReturn(deviceInfo);
    }

    @Test
    @DisplayName("Authenticated 시 RT·device 쿠키를 심고 /callback으로 파라미터 없이 리다이렉트한다")
    void authenticatedSetsCookiesAndRedirectsCleanly() throws Exception {
        SignInResult signInResult = SignInResult.of(
                AuthToken.of("access-token", RT), DeviceFixture.DEFAULT_WEB_FINGERPRINT);
        given(authService.processSocialLogin(userInfo, deviceInfo))
                .willReturn(SocialSignInResult.Authenticated.of(signInResult));

        handler.onAuthenticationSuccess(request, response, authentication);

        String setCookies = String.join("\n", response.getHeaders(AuthConst.HEADER_SET_COOKIE));
        assertThat(setCookies).contains(AuthConst.COOKIE_REFRESH_TOKEN_NAME + "=" + RT);
        assertThat(setCookies).contains(AuthConst.COOKIE_DEVICE_ID_NAME + "=" + DeviceFixture.DEFAULT_WEB_FINGERPRINT);
        assertThat(setCookies).contains("HttpOnly");

        assertThat(response.getRedirectedUrl()).isEqualTo(FRONT + OAuth2Const.FRONT_CALLBACK_PATH);
        assertThat(response.getRedirectedUrl()).doesNotContain("access-token");
        assertThat(response.getRedirectedUrl()).doesNotContain(RT);
        assertThat(response.getRedirectedUrl()).doesNotContain("code");
    }

    @Test
    @DisplayName("LinkSuggested 시 social_signup_token 쿠키를 심고 URL엔 토큰을 노출하지 않는다")
    void linkSuggestedSetsCookieAndHidesToken() throws Exception {
        given(authService.processSocialLogin(userInfo, deviceInfo))
                .willReturn(SocialSignInResult.LinkSuggested.of("kakao", "kakao-1", "nick"));
        given(socialSignUpTokenUtil.issue("kakao", "kakao-1", "nick")).willReturn("temp-token-value");

        handler.onAuthenticationSuccess(request, response, authentication);

        String setCookies = String.join("\n", response.getHeaders(AuthConst.HEADER_SET_COOKIE));
        assertThat(setCookies).contains(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME + "=temp-token-value");
        assertThat(setCookies).contains("Path=" + OAuth2Const.SOCIAL_SIGNUP_COOKIE_PATH);

        assertThat(response.getRedirectedUrl()).contains(OAuth2Const.STATUS_LINK_SUGGESTED);
        assertThat(response.getRedirectedUrl()).doesNotContain("temp-token-value");
    }

    @Test
    @DisplayName("EmailRequired 시 social_signup_token 쿠키를 심고 status=email_required로 리다이렉트한다")
    void emailRequiredSetsCookie() throws Exception {
        given(authService.processSocialLogin(userInfo, deviceInfo))
                .willReturn(SocialSignInResult.EmailRequired.of("kakao", "kakao-2", "nick2"));
        given(socialSignUpTokenUtil.issue("kakao", "kakao-2", "nick2")).willReturn("temp-2");

        handler.onAuthenticationSuccess(request, response, authentication);

        String setCookies = String.join("\n", response.getHeaders(AuthConst.HEADER_SET_COOKIE));
        assertThat(setCookies).contains(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME + "=temp-2");
        assertThat(response.getRedirectedUrl()).contains(OAuth2Const.STATUS_EMAIL_REQUIRED);
        assertThat(response.getRedirectedUrl()).doesNotContain("temp-2");
    }

}