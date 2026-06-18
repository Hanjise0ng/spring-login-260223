package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.security.token.SignUpTokenCookieManager;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SocialAuthExchange {

    private final DeviceInfoProvider deviceInfoProvider;
    private final TokenTransportResolver tokenTransportResolver;
    private final SignUpTokenCookieManager signUpTokenCookieManager;

    public String extractSignUpToken(HttpServletRequest request) {
        return signUpTokenCookieManager.read(request)
                .orElseThrow(() -> new CustomException(SocialResponseStatus.SOCIAL_SIGNUP_TOKEN_INVALID));
    }

    public DeviceInfo extractDeviceInfo(HttpServletRequest request) {
        return deviceInfoProvider.get(request);
    }

    public ResponseEntity<? extends BaseResponse<?>> writeResult(
            SocialSignInResult result, HttpServletRequest request, HttpServletResponse response) {

        return switch (result) {
            case SocialSignInResult.Authenticated auth -> {
                writeTokens(request, response, auth.getSignInResult());
                signUpTokenCookieManager.clear(response);
                yield BaseResponse.success();
            }
            case SocialSignInResult.LinkSuggested ignored ->
                    BaseResponse.success(Map.of(OAuth2Const.PARAM_STATUS, OAuth2Const.STATUS_LINK_SUGGESTED));
            case SocialSignInResult.EmailRequired ignored ->
                    throw new IllegalStateException("Social sign-up completion cannot return EmailRequired");
        };
    }

    private void writeTokens(HttpServletRequest request, HttpServletResponse response, SignInResult signInResult) {
        TokenTransport transport = tokenTransportResolver.resolve(request);
        transport.write(response, signInResult.getTokens());
        transport.writeDeviceCookie(response, signInResult.getDeviceFingerprint());
    }

    public void clearSignUpToken(HttpServletResponse response) {
        signUpTokenCookieManager.clear(response);
    }

}