package com.han.back.controller.docs;

import com.han.back.domain.auth.credential.dto.request.LocalCredentialCreateRequestDto;
import com.han.back.domain.auth.credential.dto.response.LinkedCredentialResponseDto;
import com.han.back.domain.auth.credential.dto.response.SocialLinkStartResponseDto;
import com.han.back.domain.auth.credential.exception.CredentialResponseStatus;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.global.docs.ApiErrorCode;
import com.han.back.global.docs.ApiErrorCodes;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "Credential Link", description = "소셜 연동 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface CredentialLinkApiDocs {

    @Operation(summary = "연동된 소셜 목록 조회",
            description = "현재 로그인한 계정에 연동된 소셜 제공자 목록을 반환합니다. 로컬 계정(본체)은 제외됩니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL")
    })
    ResponseEntity<BaseResponse<List<LinkedCredentialResponseDto>>> getLinkedSocials(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "소셜 연동 해제",
            description = "현재 로그인한 계정에서 지정한 소셜 제공자 연동을 해제합니다. "
                    + "로컬 계정(본체)이 남아 있으므로 모든 소셜을 해제해도 로그인이 가능합니다.")
    @ApiResponse(responseCode = "200", description = "해제 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL"),
            @ApiErrorCode(value = CredentialResponseStatus.class, constant = "CREDENTIAL_NOT_LINKED"),
            @ApiErrorCode(value = CredentialResponseStatus.class, constant = "CREDENTIAL_SOCIAL_ONLY_ACCOUNT")
    })
    ResponseEntity<BaseResponse<Empty>> unlinkSocial(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "해제할 소셜 제공자", example = "KAKAO") AuthProvider provider);

    @Operation(summary = "로컬 계정 생성 (소셜 단독 계정 승격)",
            description = "소셜 전용 계정에 로컬 로그인 수단을 추가하여 일반 계정으로 승격합니다. "
                    + "기존 소셜 연동은 유지되며, 계정 식별자(userId)는 변경되지 않습니다.")
    @ApiResponse(responseCode = "200", description = "승격 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_NOT_COMPLETED"),
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL"),
            @ApiErrorCode(value = CredentialResponseStatus.class, constant = "CREDENTIAL_LOCAL_ALREADY_EXISTS"),
            @ApiErrorCode(value = AccountResponseStatus.class, constant = "ACCOUNT_DUPLICATE_LOGIN_ID")
    })
    ResponseEntity<BaseResponse<Empty>> promoteToLocal(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid LocalCredentialCreateRequestDto request);

    @Operation(summary = "소셜 추가 연동 시작",
            description = """
                    로그인된 사용자가 새 소셜 계정을 연동하기 위한 시작점입니다.
                    응답의 link_token을 연동 전용 OAuth 시작 요청에 실어 보냅니다:
                    GET /oauth2/authorization/{provider}-link?link_token={link_token}""")
    @ApiResponse(responseCode = "200", description = "link_token 발급")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL")
    })
    ResponseEntity<BaseResponse<SocialLinkStartResponseDto>> startSocialLink(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

}