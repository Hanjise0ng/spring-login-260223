package com.han.back.controller.docs;

import com.fasterxml.jackson.annotation.JsonView;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.global.docs.ApiErrorCode;
import com.han.back.global.docs.ApiErrorCodes;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.response.ResponseView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth", description = "인증/인가 API")
public interface AuthApiDocs {

    @Operation(summary = "로그인 ID 중복 확인",
            description = "회원가입 전 로그인 ID 사용 가능 여부를 확인합니다. "
                    + "사용 가능한 경우 loginIdToken이 발급되며, 회원가입 시 이 토큰이 필요합니다.")
    @ApiResponse(responseCode = "200", description = "사용 가능 — loginIdToken 발급")
    @ApiErrorCodes({
            @ApiErrorCode(value = AccountResponseStatus.class, constant = "ACCOUNT_DUPLICATE_LOGIN_ID")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<LoginIdCheckResponseDto>> checkLoginId(
            @Parameter(description = "확인할 로그인 ID", example = "testuser01")
            @NotBlank String loginId);

    @Operation(summary = "회원가입",
            description = "로그인 ID 중복 확인(loginIdToken) + 이메일 인증 완료 후 회원가입을 진행합니다.")
    @ApiResponse(responseCode = "200", description = "회원가입 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_LOGIN_ID_CHECK_REQUIRED"),
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_NOT_COMPLETED"),
            @ApiErrorCode(value = AccountResponseStatus.class, constant = "ACCOUNT_DUPLICATE_LOGIN_ID"),
            @ApiErrorCode(value = AccountResponseStatus.class, constant = "ACCOUNT_DUPLICATE_EMAIL")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<Empty>> signUp(@Valid SignUpRequestDto requestBody);

    @Operation(summary = "토큰 재발급",
            description = """
                    유효한 Refresh Token을 전송하면 새로운 AT/RT 쌍이 발급됩니다. (세션 롤링 적용)
                    
                    - RT: refresh_token 쿠키로 전송 (Access Token은 전송하지 않음)
                    - 응답: 새 AT(헤더) + 새 RT(쿠키)""")
    @ApiResponse(responseCode = "200", description = "재발급 성공 — 새 AT(헤더), RT(쿠키)")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_REFRESH_TOKEN_MISSING"),
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_REFRESH_TOKEN_EXPIRED"),
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_REFRESH_TOKEN_INVALID")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<Empty>> reissue(HttpServletRequest request, HttpServletResponse response);

}