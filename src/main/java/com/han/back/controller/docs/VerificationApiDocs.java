package com.han.back.controller.docs;

import com.fasterxml.jackson.annotation.JsonView;
import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.global.docs.ApiErrorCode;
import com.han.back.global.docs.ApiErrorCodes;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.response.ResponseView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Verification", description = "인증 코드 발송/확인 API")
public interface VerificationApiDocs {

    @Operation(summary = "인증 코드 발송",
            description = "이메일 또는 SMS로 인증 코드를 발송합니다. "
                    + "쿨다운 기간 내 재요청 시 429가 반환됩니다.")
    @ApiResponse(responseCode = "200", description = "발송 성공 — 만료 시간 포함")
    @ApiErrorCodes({
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_CHANNEL_UNSUPPORTED"),
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_COOLDOWN"),
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_MAIL_TEMPLATE_FAIL"),
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_MAIL_SEND_FAIL"),
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_SMS_SEND_FAIL")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<VerificationSendResponseDto>> sendCode(
            @Valid VerificationSendRequestDto request);

    @Operation(summary = "인증 코드 확인",
            description = "발송된 인증 코드를 검증합니다. "
                    + "인증 성공 시 해당 대상 + 인증 용도 조합이 '인증 완료' 상태로 저장됩니다.")
    @ApiResponse(responseCode = "200", description = "인증 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_CODE_MISMATCH"),
            @ApiErrorCode(value = VerificationResponseStatus.class, constant = "VERIFY_CODE_EXPIRED")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<Empty>> confirmCode(
            @Valid VerificationConfirmRequestDto request);

}