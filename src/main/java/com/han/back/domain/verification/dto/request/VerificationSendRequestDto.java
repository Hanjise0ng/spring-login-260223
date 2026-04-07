package com.han.back.domain.verification.dto.request;

import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.notification.NotificationChannel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "인증 코드 발송 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VerificationSendRequestDto {

    @Schema(description = "인증 대상 (이메일 또는 전화번호)", example = "user@example.com")
    @NotBlank(message = "Target is required.")
    private String target;

    @Schema(description = "인증 용도", example = "SIGN_UP")
    @NotNull(message = "Verification type is required.")
    private VerificationType type;

    @Schema(description = "발송 채널", example = "EMAIL")
    @NotNull(message = "Notification channel is required.")
    private NotificationChannel channel;

    public static VerificationSendRequestDto of(
            String target, VerificationType type, NotificationChannel channel) {
        return new VerificationSendRequestDto(target, type, channel);
    }

}