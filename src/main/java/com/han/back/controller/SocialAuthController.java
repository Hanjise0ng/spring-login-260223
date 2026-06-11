package com.han.back.controller;

import com.han.back.domain.auth.dto.OAuth2CodePayload;
import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.request.OAuth2SignUpCompleteRequestDto;
import com.han.back.domain.auth.oauth2.service.OAuth2CodeService;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final OAuth2CodeService oauth2CodeService;


    @Operation(summary = "OAuth2 인가 코드 교환",
            description = """
                    소셜 로그인 콜백 후 발급된 일회성 code를 토큰으로 교환합니다.
                    
                    - code: 소셜 로그인 성공 시 서버가 발급한 일회성 코드 (1회 소비)
                    - 응답: 새 AT(헤더) + 새 RT(쿠키) + 디바이스 쿠키""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교환 성공 — AT(헤더), RT(쿠키) 발급"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAIL: code 누락 또는 형식 오류"),
            @ApiResponse(responseCode = "401", description = "AUTH_AUTHENTICATION_FAIL: 유효하지 않거나 이미 사용된 code")
    })
    @GetMapping("/token")
    public ResponseEntity<BaseResponse<Empty>> exchangeOAuth2Code(
            @Parameter(description = "소셜 로그인 성공 시 발급된 일회성 코드", example = "a1b2c3d4-...")
            @RequestParam String code,
            HttpServletRequest request, HttpServletResponse response) {

        OAuth2CodePayload payload = oauth2CodeService.consume(code);

        TokenTransport transport = tokenTransportResolver.resolve(request);
        transport.write(response, AuthToken.of(payload.getAccessToken(), payload.getRefreshToken()));
        transport.writeDeviceCookie(response, payload.getDeviceFingerprint());

        return BaseResponse.success();
    }

    @Operation(summary = "소셜 회원가입 완료",
            description = """
                    이메일을 제공하지 않는 소셜 계정의 최초 가입을 완료합니다.
                    임시 토큰(tempToken)과 사용자가 입력한 이메일로 가입을 마무리하고 일회성 code를 발급합니다.
                    
                    - tempToken: 소셜 로그인 시 이메일 부재로 발급된 임시 가입 토큰
                    - email: 사용자가 입력/인증한 이메일
                    - 응답: code (이후 /oauth2/token으로 교환)""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 완료 — 교환용 code 발급"),
            @ApiResponse(responseCode = "400", description = """
                    - VALIDATION_FAIL: 유효성 검증 실패
                    - VERIFY_NOT_COMPLETED: 이메일 인증 미완료"""),
            @ApiResponse(responseCode = "401", description = "SOCIAL_SIGNUP_TOKEN_INVALID: 임시 가입 토큰이 유효하지 않거나 만료됨"),
            @ApiResponse(responseCode = "409", description = """
                    - ACCOUNT_DUPLICATE_EMAIL: 이미 사용 중인 이메일(LOCAL)
                    - SOCIAL_ALREADY_LINKED: 이미 연동된 소셜 계정""")
    })
    @PostMapping("/complete")
    public ResponseEntity<BaseResponse<Map<String, String>>> completeSocialSignUp(
            @RequestBody @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest) {

        DeviceInfo deviceInfo = deviceInfoProvider.get(httpRequest);

        SignInResult signInResult = authService.completeSocialSignUp(
                request.getTempToken(), request.getEmail(), deviceInfo);
        String code = oauth2CodeService.save(signInResult);

        return BaseResponse.success(Map.of("code", code));
    }

}