package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.util.SocialSignUpTokenUtil;
import com.han.back.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
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
    private final SignUpTokenCookieManager signUpTokenCookieManager;
    private final DeviceInfoProvider deviceInfoProvider;
    private final SocialLinkContext socialLinkContext;
    private final CredentialLinkService credentialLinkService;

    @Value("${app.front-base-url}")
    private String frontBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = token.getPrincipal();
        OAuth2UserInfo userInfo = (OAuth2UserInfo) oAuth2User.getAttributes().get(OAuth2Const.ATTR_USER_INFO);

        if (isLinkFlow(token)) {
            handleSocialLink(request, response, userInfo);
            return;
        }

        handleSocialLogin(request, response, userInfo);
    }

    private boolean isLinkFlow(OAuth2AuthenticationToken token) {
        return token.getAuthorizedClientRegistrationId().endsWith(OAuth2Const.REGISTRATION_LINK_SUFFIX);
    }

    private void handleSocialLink(HttpServletRequest request, HttpServletResponse response, OAuth2UserInfo userInfo)
            throws IOException {
        try {
            Long userId = socialLinkContext.consume(request.getParameter(OAuth2Const.PARAM_STATE))
                    .orElseThrow(() -> new CustomException(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID));

            AuthProvider provider = userInfo.getProvider();
            credentialLinkService.linkSocialCredential(userId, provider, userInfo.getProviderId());

            log.info("Social Linked - UserPK: {} | Provider: {}", userId, provider);

            redirectToLinkResult(response, Map.of(OAuth2Const.PARAM_STATUS, OAuth2Const.STATUS_LINK_SUCCESS));

        } catch (CustomException e) {
            log.warn("Social Link Failed - Code: {}", e.getStatus().getCode());
            redirectToLinkResult(response, Map.of(OAuth2Const.PARAM_ERROR, e.getStatus().getCode()));
        }
    }

    private void handleSocialLogin(HttpServletRequest request, HttpServletResponse response, OAuth2UserInfo userInfo)
            throws IOException {
        DeviceInfo deviceInfo = deviceInfoProvider.get(request);
        SocialSignInResult result = authService.processSocialLogin(userInfo, deviceInfo);

        switch (result) {
            case SocialSignInResult.Authenticated auth -> {
                writeRefreshAndDeviceCookies(response, auth.getSignInResult());
                redirectToCallback(response, Map.of());
            }
            case SocialSignInResult.EmailRequired info -> {
                issueSignUpTokenCookie(response, info.getProvider(), info.getProviderId(), info.getNickname());
                redirectToCallback(response, Map.of(OAuth2Const.PARAM_STATUS, OAuth2Const.STATUS_EMAIL_REQUIRED));
            }
            case SocialSignInResult.LinkSuggested link -> {
                issueSignUpTokenCookie(response, link.getProvider(), link.getProviderId(), link.getNickname());
                redirectToCallback(response, Map.of(OAuth2Const.PARAM_STATUS, OAuth2Const.STATUS_LINK_SUGGESTED));
            }
        }
    }

    private void issueSignUpTokenCookie(HttpServletResponse response, String provider, String providerId, String nickname) {
        String tempToken = socialSignUpTokenUtil.issue(provider, providerId, nickname);
        signUpTokenCookieManager.write(response, tempToken);
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

    private void redirectToCallback(HttpServletResponse response, Map<String, String> params) throws IOException {
        response.sendRedirect(buildUrl(OAuth2Const.FRONT_CALLBACK_PATH, params));
    }

    private void redirectToLinkResult(HttpServletResponse response, Map<String, String> params) throws IOException {
        response.sendRedirect(buildUrl(OAuth2Const.FRONT_LINK_COMPLETE_PATH, params));
    }

    private String buildUrl(String path, Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(frontBaseUrl).path(path);
        params.forEach(builder::queryParam);

        return builder.build().encode().toUriString();
    }

}