package com.han.back.domain.verification.dto.request;

import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.infra.notification.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "인증 코드 발송 요청")
@Getter
@AllArgsConstructor
public class VerificationSendRequestDto {

    @Schema(description = "인증 대상 (이메일 또는 전화번호)", example = "user@example.com")
    @NotBlank(message = "Target is required.")
    private final String target;

    @Schema(description = "인증 용도", example = "SIGN_UP")
    @NotNull(message = "Verification type is required.")
    private final VerificationType type;

    @Schema(description = "발송 채널", example = "EMAIL")
    @NotNull(message = "Notification channel is required.")
    private final NotificationChannel channel;

}