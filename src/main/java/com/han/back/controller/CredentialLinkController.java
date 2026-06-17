package com.han.back.controller;

import com.han.back.domain.auth.credential.dto.request.LocalCredentialCreateRequestDto;
import com.han.back.domain.auth.credential.dto.response.LinkedCredentialResponseDto;
import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/credentials")
@RequiredArgsConstructor
@Tag(name = "Credential Link", description = "소셜 연동 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class CredentialLinkController {

    private final CredentialLinkService credentialLinkService;

    @Operation(summary = "연동된 소셜 목록 조회",
            description = "현재 로그인한 계정에 연동된 소셜 제공자 목록을 반환합니다. 로컬 계정(본체)은 제외됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "AUTH_AUTHENTICATION_FAIL: 인증 실패")
    })
    @GetMapping("/socials")
    public ResponseEntity<BaseResponse<List<LinkedCredentialResponseDto>>> getLinkedSocials(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return BaseResponse.success(credentialLinkService.getLinkedSocials(userDetails.getId()));
    }

    @Operation(summary = "소셜 연동 해제",
            description = "현재 로그인한 계정에서 지정한 소셜 제공자 연동을 해제합니다. "
                    + "로컬 계정(본체)이 남아 있으므로 모든 소셜을 해제해도 로그인이 가능합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "해제 성공"),
            @ApiResponse(responseCode = "401", description = "AUTH_AUTHENTICATION_FAIL: 인증 실패"),
            @ApiResponse(responseCode = "404", description = "CREDENTIAL_NOT_LINKED: 연동되지 않은 소셜"),
            @ApiResponse(responseCode = "409", description = "CREDENTIAL_SOCIAL_ONLY_ACCOUNT: 소셜 전용 계정")
    })
    @DeleteMapping("/socials/{provider}")
    public ResponseEntity<BaseResponse<Empty>> unlinkSocial(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "해제할 소셜 제공자", example = "KAKAO")
            @PathVariable AuthProvider provider) {

        credentialLinkService.unlinkSocialCredential(userDetails.getId(), provider);
        return BaseResponse.success();
    }

    @Operation(summary = "로컬 계정 생성 (소셜 단독 계정 승격)",
            description = "소셜 전용 계정에 로컬 로그인 수단을 추가하여 일반 계정으로 승격합니다. "
                    + "기존 소셜 연동은 유지되며, 계정 식별자(userId)는 변경되지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "승격 성공"),
            @ApiResponse(responseCode = "400", description = "VERIFY_NOT_COMPLETED: 이메일 인증 미완료"),
            @ApiResponse(responseCode = "401", description = "AUTH_AUTHENTICATION_FAIL: 인증 실패"),
            @ApiResponse(responseCode = "409", description = """
                    - CREDENTIAL_LOCAL_ALREADY_EXISTS: 이미 로컬 계정 존재
                    - ACCOUNT_DUPLICATE_LOGIN_ID: 아이디 중복""")
    })
    @PostMapping("/local")
    public ResponseEntity<BaseResponse<Empty>> promoteToLocal(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid LocalCredentialCreateRequestDto request) {

        credentialLinkService.promoteToLocalAccount(userDetails.getId(), request);
        return BaseResponse.success();
    }

}