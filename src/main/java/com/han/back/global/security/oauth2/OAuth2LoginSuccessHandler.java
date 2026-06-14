package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.util.SocialSignUpTokenUtil;
import com.han.back.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;
    private final SocialSignUpTokenUtil socialSignUpTokenUtil;
    private final DeviceInfoProvider deviceInfoProvider;

    @Value("${app.front-base-url}")
    private String frontBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2UserInfo userInfo = (OAuth2UserInfo) oAuth2User.getAttributes().get(OAuth2Const.ATTR_USER_INFO);

        DeviceInfo deviceInfo = deviceInfoProvider.get(request);
        SocialSignInResult result = authService.processSocialLogin(userInfo, deviceInfo);

        switch (result) {
            case SocialSignInResult.Authenticated auth -> {
                writeRefreshAndDeviceCookies(response, auth.getSignInResult());
                response.sendRedirect(buildUrl(OAuth2Const.FRONT_CALLBACK_PATH, Map.of()));
            }
            case SocialSignInResult.EmailRequired info -> {
                String tempToken = socialSignUpTokenUtil.issue(
                        info.getProvider(), info.getProviderId(), info.getNickname());
                writeSignUpTokenCookie(response, tempToken);
                response.sendRedirect(buildUrl(OAuth2Const.FRONT_CALLBACK_PATH,
                        Map.of(OAuth2Const.PARAM_STATUS, OAuth2Const.STATUS_EMAIL_REQUIRED)));
            }
            case SocialSignInResult.LinkSuggested link -> {
                String tempToken = socialSignUpTokenUtil.issue(
                        link.getProvider(), link.getProviderId(), link.getNickname());
                writeSignUpTokenCookie(response, tempToken);
                response.sendRedirect(buildUrl(OAuth2Const.FRONT_CALLBACK_PATH,
                        Map.of(OAuth2Const.PARAM_STATUS, OAuth2Const.STATUS_LINK_SUGGESTED)));
            }
        }
    }

    private void writeRefreshAndDeviceCookies(HttpServletResponse response, SignInResult signInResult) {
        CookieUtil.addSecureCookie(response,
                AuthConst.COOKIE_REFRESH_TOKEN_NAME,
                signInResult.getTokens().getRefreshToken(),
                AuthConst.REFRESH_TOKEN_TTL);
        CookieUtil.addSecureCookie(response,
                AuthConst.COOKIE_DEVICE_ID_NAME,
                signInResult.getDeviceFingerprint(),
                AuthConst.DEVICE_COOKIE_TTL);
    }

    private void writeSignUpTokenCookie(HttpServletResponse response, String tempToken) {
        CookieUtil.addSecureCookie(response,
                OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME,
                tempToken,
                OAuth2Const.SOCIAL_SIGN_UP_TOKEN_TTL,
                OAuth2Const.SOCIAL_SIGNUP_COOKIE_PATH);
    }

    private String buildUrl(String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontBaseUrl).path(path);
        params.forEach(builder::queryParam);

        return builder.build().encode().toUriString();
    }

}