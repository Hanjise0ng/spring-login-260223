package com.han.back.domain.verification.dto.request;

import com.han.back.domain.verification.entity.VerificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "인증 코드 확인 요청")
@Getter
@AllArgsConstructor
public class VerificationConfirmRequestDto {

    @Schema(description = "인증 대상 (이메일 또는 전화번호)", example = "user@example.com")
    @NotBlank(message = "Target is required.")
    private final String target;

    @Schema(description = "수신한 인증 코드", example = "482917")
    @NotBlank(message = "Verification code is required.")
    private final String code;

    @Schema(description = "인증 용도", example = "SIGN_UP")
    @NotNull(message = "Verification type is required.")
    private final VerificationType type;

}