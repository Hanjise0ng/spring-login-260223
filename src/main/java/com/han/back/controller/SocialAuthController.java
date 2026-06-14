package com.han.back.controller;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.dto.request.OAuth2SignUpCompleteRequestDto;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import com.han.back.global.util.CookieUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Auth", description = "소셜 인증 API")
public class SocialAuthController {

    private final AuthService authService;
    private final DeviceInfoProvider deviceInfoProvider;
    private final TokenTransportResolver tokenTransportResolver;

    @Operation(summary = "소셜 회원가입 완료",
            description = """
                    이메일을 제공하지 않는 소셜 계정의 최초 가입을 완료합니다.
                    임시 토큰은 HttpOnly 쿠키(social_signup_token)로 전달되며, 사용자가 입력한 이메일로 가입을 마무리합니다.
                    
                    - 가입 성공: AT(헤더) + RT(쿠키) + device 쿠키 직접 발급
                    - 이메일이 기존 LOCAL 계정과 충돌: status=link_suggested 반환 (연동 유도)""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = """
                    - 가입 완료: AT(헤더), RT(쿠키) 발급
                    - 연동 유도: { status: "link_suggested" }"""),
            @ApiResponse(responseCode = "400", description = """
                    - VALIDATION_FAIL: 유효성 검증 실패
                    - VERIFY_NOT_COMPLETED: 이메일 인증 미완료"""),
            @ApiResponse(responseCode = "401", description = "SOCIAL_SIGNUP_TOKEN_INVALID: 임시 가입 토큰이 유효하지 않거나 만료됨"),
            @ApiResponse(responseCode = "409", description = "SOCIAL_ALREADY_LINKED: 이미 연동된 소셜 계정")
    })
    @PostMapping("/complete")
    public ResponseEntity<? extends BaseResponse<?>> completeSocialSignUp(
            @RequestBody @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String tempToken = CookieUtil.getCookieValue(httpRequest, OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME)
                .orElseThrow(() -> new CustomException(SocialResponseStatus.SOCIAL_SIGNUP_TOKEN_INVALID));

        DeviceInfo deviceInfo = deviceInfoProvider.get(httpRequest);

        SocialSignInResult result = authService.completeSocialSignUp(tempToken, request.getEmail(), deviceInfo);

        switch (result) {
            case SocialSignInResult.Authenticated auth -> {
                writeTokens(httpRequest, httpResponse, auth.getSignInResult());
                clearSignUpTokenCookie(httpResponse);
                return BaseResponse.success();
            }
            case SocialSignInResult.LinkSuggested link -> {
                return BaseResponse.success(Map.of(OAuth2Const.PARAM_STATUS, OAuth2Const.STATUS_LINK_SUGGESTED));
            }
            case SocialSignInResult.EmailRequired emailRequired ->
                    throw new IllegalStateException("completeSocialSignUp cannot return EmailRequired");
        }
    }

    private void writeTokens(HttpServletRequest request, HttpServletResponse response, SignInResult signInResult) {
        TokenTransport transport = tokenTransportResolver.resolve(request);
        transport.write(response, signInResult.getTokens());
        transport.writeDeviceCookie(response, signInResult.getDeviceFingerprint());
    }

    private void clearSignUpTokenCookie(HttpServletResponse response) {
        CookieUtil.addSecureCookie(response,
                OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME, "",
                java.time.Duration.ZERO,
                OAuth2Const.SOCIAL_SIGNUP_COOKIE_PATH);
    }

}