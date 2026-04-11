package com.han.back.domain.verification.dto.request;

import com.han.back.domain.verification.entity.VerificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "인증 코드 확인 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VerificationConfirmRequestDto {

    @Schema(description = "인증 대상 (이메일 또는 전화번호)", example = "user@example.com")
    @NotBlank(message = "Target is required.")
    private String target;

    @Schema(description = "수신한 인증 코드", example = "482917")
    @NotBlank(message = "Verification code is required.")
    private String code;

    @Schema(description = "인증 용도", example = "SIGN_UP")
    @NotNull(message = "Verification type is required.")
    private VerificationType type;

    public static VerificationConfirmRequestDto of(String target, String code, VerificationType type) {
        return new VerificationConfirmRequestDto(target, code, type);
    }

}