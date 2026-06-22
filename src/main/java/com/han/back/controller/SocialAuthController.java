package com.han.back.controller;

import com.han.back.controller.docs.SocialAuthApiDocs;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.dto.request.OAuth2SignUpCompleteRequestDto;
import com.han.back.domain.auth.dto.request.SocialLinkRequestDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.oauth2.SignUpTokenCookieManager;
import com.han.back.global.security.oauth2.SocialSignUpResponseWriter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class SocialAuthController implements SocialAuthApiDocs {

    private final AuthService authService;
    private final DeviceInfoProvider deviceInfoProvider;
    private final SignUpTokenCookieManager signUpTokenCookieManager;
    private final SocialSignUpResponseWriter socialSignUpResponseWriter;

    @Override
    @PostMapping("/complete")
    public ResponseEntity<? extends BaseResponse<?>> completeSocialSignUp(
            @RequestBody @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String tempToken = signUpTokenCookieManager.read(httpRequest).orElse(null);
        DeviceInfo deviceInfo = deviceInfoProvider.get(httpRequest);

        SocialSignInResult result = authService.completeSocialSignUp(tempToken, request.getEmail(), deviceInfo);
        return socialSignUpResponseWriter.write(result, httpRequest, httpResponse);
    }

    @Override
    @PostMapping("/separate")
    public ResponseEntity<? extends BaseResponse<?>> createSeparateAccount(
            @RequestBody @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String tempToken = signUpTokenCookieManager.read(httpRequest).orElse(null);
        DeviceInfo deviceInfo = deviceInfoProvider.get(httpRequest);

        SocialSignInResult result = authService.createSeparateSocialAccount(tempToken, request.getEmail(), deviceInfo);
        return socialSignUpResponseWriter.write(result, httpRequest, httpResponse);
    }

    @Override
    @PostMapping("/link")
    public ResponseEntity<BaseResponse<Empty>> linkSocial(
            @RequestBody @Valid SocialLinkRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String tempToken = signUpTokenCookieManager.read(httpRequest).orElse(null);
        authService.linkSocialToLocalAccount(tempToken, request.getLoginId(), request.getPassword());

        socialSignUpResponseWriter.clearSignUpToken(httpResponse);
        return BaseResponse.success();
    }

}