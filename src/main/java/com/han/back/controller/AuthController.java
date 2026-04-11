package com.han.back.controller;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.dto.response.ReissueResponseDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.util.AuthHttpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증/인가 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인 ID 중복 확인",
            description = "회원가입 전 로그인 ID 사용 가능 여부를 확인합니다. "
                    + "사용 가능한 경우 loginIdToken이 발급되며, 회원가입 시 이 토큰이 필요합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 가능 — loginIdToken 발급"),
            @ApiResponse(responseCode = "400", description = "VF: 유효성 검증 실패"),
            @ApiResponse(responseCode = "409", description = "DI: 아이디 중복")
    })
    @GetMapping("/check-login-id")
    public ResponseEntity<BaseResponse<LoginIdCheckResponseDto>> checkLoginId(
            @Parameter(description = "확인할 로그인 ID", example = "testuser01")
            @RequestParam @NotBlank String loginId) {

        LoginIdCheckResponseDto response = authService.checkLoginId(loginId);
        return BaseResponse.success(response);
    }

    @Operation(summary = "회원가입",
            description = "로그인 ID 중복 확인(loginIdToken) + 이메일 인증 완료 후 회원가입을 진행합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = """
                    - VF: 유효성 검증 실패
                    - LICR: 로그인 ID 중복 확인 필요
                    - VNC: 인증 미완료"""),
            @ApiResponse(responseCode = "409", description = """
                    - DI: 아이디 중복
                    - DE: 이메일 중복""")
    })
    @PostMapping("/sign-up")
    public ResponseEntity<BaseResponse<Empty>> signUp(
            @RequestBody @Valid SignUpRequestDto requestBody) {

        authService.signUp(requestBody);
        return BaseResponse.success();
    }

    @Operation(summary = "토큰 재발급",
            description = """
                    만료된 Access Token과 유효한 Refresh Token을 함께 전송하면
                    새로운 AT/RT 쌍이 발급됩니다. (세션 롤링 적용)
                    
                    - AT: Authorization 헤더로 전송
                    - RT: refresh_token 쿠키로 전송
                    - 응답: 새 AT(헤더) + 새 RT(쿠키)""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공 — 새 AT(헤더), RT(쿠키)"),
            @ApiResponse(responseCode = "401", description = """
                    - MAT: Access Token 누락
                    - MRT: Refresh Token 누락
                    - ERT: Refresh Token 만료 (재로그인 필요)
                    - IRT: Refresh Token 무효""")
    })
    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<Empty>> reissue(
            HttpServletRequest request, HttpServletResponse response) {

        AuthToken oldTokens = AuthHttpUtil.extractRequiredTokenPair(request);
        ReissueResponseDto result = authService.reissue(oldTokens);
        AuthHttpUtil.setTokenResponse(response, result.getAuthToken(), result.getDeviceType());
        return BaseResponse.success();
    }

}