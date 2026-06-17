package com.han.back.controller;

import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.dto.request.OAuth2SignUpCompleteRequestDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.security.oauth2.SocialAuthExchange;
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

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Auth", description = "소셜 인증 API")
public class SocialAuthController {

    private final AuthService authService;
    private final SocialAuthExchange socialAuthExchange;

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

        String tempToken = socialAuthExchange.extractSignUpToken(httpRequest);
        DeviceInfo deviceInfo = socialAuthExchange.extractDeviceInfo(httpRequest);

        SocialSignInResult result = authService.completeSocialSignUp(tempToken, request.getEmail(), deviceInfo);
        return socialAuthExchange.writeResult(result, httpRequest, httpResponse);
    }

    @Operation(summary = "별도 계정으로 시작하기",
            description = "이메일 충돌 상황에서 기존 계정과 합치지 않고 새 소셜 전용 계정을 생성합니다. "
                    + "social_signup_token 쿠키와 이메일(요청 바디)이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "별도 계정 생성 및 로그인 완료"),
            @ApiResponse(responseCode = "400", description = "VERIFY_NOT_COMPLETED: 이메일 인증 미완료"),
            @ApiResponse(responseCode = "401", description = "SOCIAL_SIGNUP_TOKEN_INVALID: 임시 토큰 무효"),
            @ApiResponse(responseCode = "409", description = "SOCIAL_ALREADY_LINKED: 이미 사용 중인 소셜")
    })
    @PostMapping("/separate")
    public ResponseEntity<? extends BaseResponse<?>> createSeparateAccount(
            @RequestBody @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {

        String tempToken = socialAuthExchange.extractSignUpToken(httpRequest);
        DeviceInfo deviceInfo = socialAuthExchange.extractDeviceInfo(httpRequest);

        SocialSignInResult result = authService.createSeparateSocialAccount(tempToken, request.getEmail(), deviceInfo);
        return socialAuthExchange.writeResult(result, httpRequest, httpResponse);
    }

}