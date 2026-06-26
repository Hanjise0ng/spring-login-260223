package com.han.back.controller.docs;

import com.han.back.domain.auth.credential.exception.CredentialResponseStatus;
import com.han.back.domain.auth.dto.request.OAuth2SignUpCompleteRequestDto;
import com.han.back.domain.auth.dto.request.SocialLinkRequestDto;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.global.docs.ApiErrorCode;
import com.han.back.global.docs.ApiErrorCodes;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "OAuth2 Auth", description = "소셜 인증 API")
public interface SocialAuthApiDocs {

    @Operation(summary = "소셜 회원가입 완료",
            description = """
                    이메일을 제공하지 않는 소셜 계정의 최초 가입을 완료합니다.
                    임시 토큰은 HttpOnly 쿠키(social_signup_token)로 전달되며, 사용자가 입력한 이메일로 가입을 마무리합니다.

                    - 가입 성공: AT(헤더) + RT(쿠키) + device 쿠키 직접 발급
                    - 이메일이 기존 LOCAL 계정과 충돌: status=link_suggested 반환 (연동 유도)""")
    @ApiResponse(responseCode = "200", description = """
            - 가입 완료: AT(헤더), RT(쿠키) 발급
            - 연동 유도: { status: "link_suggested" }""")
    @ApiErrorCodes({
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_NOT_COMPLETED"),
            @ApiErrorCode(value = SocialResponseStatus.class, constant = "SOCIAL_SIGNUP_TOKEN_INVALID"),
            @ApiErrorCode(value = SocialResponseStatus.class, constant = "SOCIAL_ALREADY_LINKED")
    })
    ResponseEntity<? extends BaseResponse<?>> completeSocialSignUp(
            @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    @Operation(summary = "별도 계정으로 시작하기",
            description = "이메일 충돌 상황에서 기존 계정과 합치지 않고 새 소셜 전용 계정을 생성합니다. "
                    + "social_signup_token 쿠키와 이메일(요청 바디)이 필요합니다.")
    @ApiResponse(responseCode = "200", description = "별도 계정 생성 및 로그인 완료")
    @ApiErrorCodes({
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_NOT_COMPLETED"),
            @ApiErrorCode(value = SocialResponseStatus.class, constant = "SOCIAL_SIGNUP_TOKEN_INVALID"),
            @ApiErrorCode(value = SocialResponseStatus.class, constant = "SOCIAL_ALREADY_LINKED")
    })
    ResponseEntity<? extends BaseResponse<?>> createSeparateAccount(
            @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    @Operation(summary = "소셜 연동 (기존 LOCAL 계정에 추가)",
            description = """
                    이메일 충돌 상황에서 기존 LOCAL 계정에 소셜 계정을 연동합니다.
                    social_signup_token 쿠키와 LOCAL 계정 자격증명(아이디/비밀번호)이 필요합니다.

                    연동만 수행하며 로그인 토큰을 발급하지 않습니다. 연동 완료 후 사용자가 다시 소셜 로그인하면 정상 로그인됩니다.""")
    @ApiResponse(responseCode = "200", description = "연동 완료 (토큰 미발급)")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_SIGN_IN_FAIL"),
            @ApiErrorCode(value = SocialResponseStatus.class, constant = "SOCIAL_SIGNUP_TOKEN_INVALID"),
            @ApiErrorCode(value = CredentialResponseStatus.class, constant = "CREDENTIAL_PROVIDER_ALREADY_LINKED"),
            @ApiErrorCode(value = CredentialResponseStatus.class, constant = "CREDENTIAL_SOCIAL_ALREADY_USED"),
            @ApiErrorCode(value = CredentialResponseStatus.class, constant = "CREDENTIAL_SOCIAL_ONLY_ACCOUNT")
    })
    ResponseEntity<BaseResponse<Empty>> linkSocial(
            @Valid SocialLinkRequestDto request,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse);

}