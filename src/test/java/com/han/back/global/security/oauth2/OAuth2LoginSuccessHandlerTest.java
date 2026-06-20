package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.security.token.util.SocialSignUpTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2LoginSuccessHandler")
class OAuth2LoginSuccessHandlerTest {

    @Mock private AuthService authService;
    @Mock private SocialSignUpTokenUtil socialSignUpTokenUtil;
    @Mock private SignUpTokenCookieManager signUpTokenCookieManager;
    @Mock private DeviceInfoProvider deviceInfoProvider;
    @Mock private SocialLinkContext socialLinkContext;
    @Mock private CredentialLinkService credentialLinkService;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private OAuth2AuthenticationToken authentication;
    @Mock private OAuth2User oAuth2User;
    @Mock private OAuth2UserInfo userInfo;

    private OAuth2LoginSuccessHandler successHandler;

    private static final String FRONT_BASE_URL = "http://localhost:3000";

    @BeforeEach
    void setUp() {
        successHandler = new OAuth2LoginSuccessHandler(
                authService, socialSignUpTokenUtil, signUpTokenCookieManager,
                deviceInfoProvider, socialLinkContext, credentialLinkService);
        ReflectionTestUtils.setField(successHandler, "frontBaseUrl", FRONT_BASE_URL);

        given(authentication.getPrincipal()).willReturn(oAuth2User);
        given(oAuth2User.getAttributes()).willReturn(Map.of(OAuth2Const.ATTR_USER_INFO, userInfo));
    }

    @Nested
    @DisplayName("연동 흐름 (registrationId가 -link)")
    class LinkFlow {

        @Test
        @DisplayName("컨텍스트에서 userId를 복원해 연동하고 성공 페이지로 리다이렉트한다")
        void linksAndRedirectsSuccess() throws Exception {
            given(authentication.getAuthorizedClientRegistrationId()).willReturn("kakao-link");
            given(request.getParameter(OAuth2Const.PARAM_STATE)).willReturn("state-1");
            given(socialLinkContext.consume("state-1")).willReturn(Optional.of(4L));
            given(userInfo.getProvider()).willReturn(AuthProvider.KAKAO);
            given(userInfo.getProviderId()).willReturn("kakao-123");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(credentialLinkService).linkSocialCredential(4L, AuthProvider.KAKAO, "kakao-123");
            verify(response).sendRedirect(contains(OAuth2Const.STATUS_LINK_SUCCESS));
        }

        @Test
        @DisplayName("연동 컨텍스트가 없으면 연동하지 않고 에러 페이지로 리다이렉트한다")
        void noContextRedirectsError() throws Exception {
            given(authentication.getAuthorizedClientRegistrationId()).willReturn("kakao-link");
            given(request.getParameter(OAuth2Const.PARAM_STATE)).willReturn("state-1");
            given(socialLinkContext.consume("state-1")).willReturn(Optional.empty());

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(credentialLinkService, never()).linkSocialCredential(any(), any(), any());
            verify(response).sendRedirect(contains(OAuth2Const.PARAM_ERROR + "="));
        }

        @Test
        @DisplayName("연동 실패(이미 연동 등) 시 에러 코드를 담아 리다이렉트한다")
        void linkFailureRedirectsError() throws Exception {
            given(authentication.getAuthorizedClientRegistrationId()).willReturn("kakao-link");
            given(request.getParameter(OAuth2Const.PARAM_STATE)).willReturn("state-1");
            given(socialLinkContext.consume("state-1")).willReturn(Optional.of(4L));
            given(userInfo.getProvider()).willReturn(AuthProvider.KAKAO);
            given(userInfo.getProviderId()).willReturn("kakao-123");
            willThrow(new com.han.back.global.exception.CustomException(
                    com.han.back.domain.auth.oauth2.exception.SocialResponseStatus.SOCIAL_ALREADY_LINKED))
                    .given(credentialLinkService).linkSocialCredential(4L, AuthProvider.KAKAO, "kakao-123");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(response).sendRedirect(contains(OAuth2Const.PARAM_ERROR + "="));
        }
    }

    @Nested
    @DisplayName("로그인 흐름 (registrationId가 -link 아님)")
    class LoginFlow {

        @Test
        @DisplayName("Authenticated면 토큰 쿠키를 발급하고 콜백으로 리다이렉트한다")
        void authenticatedWritesCookies() throws Exception {
            given(authentication.getAuthorizedClientRegistrationId()).willReturn("kakao");
            DeviceInfo deviceInfo = mock(DeviceInfo.class);
            given(deviceInfoProvider.get(request)).willReturn(deviceInfo);

            SocialSignInResult.Authenticated result = mock(SocialSignInResult.Authenticated.class);
            SignInResult signInResult = mock(SignInResult.class, RETURNS_DEEP_STUBS);
            given(result.getSignInResult()).willReturn(signInResult);
            given(signInResult.getTokens().getRefreshToken()).willReturn("refresh-token");
            given(signInResult.getDeviceFingerprint()).willReturn("fingerprint");
            given(authService.processSocialLogin(userInfo, deviceInfo)).willReturn(result);

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(authService).processSocialLogin(userInfo, deviceInfo);
            verify(credentialLinkService, never()).linkSocialCredential(any(), any(), any());
            verify(response).sendRedirect(contains(OAuth2Const.FRONT_CALLBACK_PATH));
        }

        @Test
        @DisplayName("EmailRequired면 가입 토큰 쿠키를 발급하고 email_required로 리다이렉트한다")
        void emailRequiredIssuesSignUpToken() throws Exception {
            given(authentication.getAuthorizedClientRegistrationId()).willReturn("kakao");
            DeviceInfo deviceInfo = mock(DeviceInfo.class);
            given(deviceInfoProvider.get(request)).willReturn(deviceInfo);

            SocialSignInResult.EmailRequired result = mock(SocialSignInResult.EmailRequired.class);
            given(result.getProvider()).willReturn("KAKAO");
            given(result.getProviderId()).willReturn("kakao-123");
            given(result.getNickname()).willReturn("닉네임");
            given(authService.processSocialLogin(userInfo, deviceInfo)).willReturn(result);
            given(socialSignUpTokenUtil.issue("KAKAO", "kakao-123", "닉네임")).willReturn("signup-token");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(signUpTokenCookieManager).write(response, "signup-token");
            verify(response).sendRedirect(contains(OAuth2Const.STATUS_EMAIL_REQUIRED));
        }

        @Test
        @DisplayName("LinkSuggested면 가입 토큰 쿠키를 발급하고 link_suggested로 리다이렉트한다")
        void linkSuggestedIssuesSignUpToken() throws Exception {
            given(authentication.getAuthorizedClientRegistrationId()).willReturn("kakao");
            DeviceInfo deviceInfo = mock(DeviceInfo.class);
            given(deviceInfoProvider.get(request)).willReturn(deviceInfo);

            SocialSignInResult.LinkSuggested result = mock(SocialSignInResult.LinkSuggested.class);
            given(result.getProvider()).willReturn("KAKAO");
            given(result.getProviderId()).willReturn("kakao-123");
            given(result.getNickname()).willReturn("닉네임");
            given(authService.processSocialLogin(userInfo, deviceInfo)).willReturn(result);
            given(socialSignUpTokenUtil.issue("KAKAO", "kakao-123", "닉네임")).willReturn("signup-token");

            successHandler.onAuthenticationSuccess(request, response, authentication);

            verify(signUpTokenCookieManager).write(response, "signup-token");
            verify(response).sendRedirect(contains(OAuth2Const.STATUS_LINK_SUGGESTED));
        }
    }

}