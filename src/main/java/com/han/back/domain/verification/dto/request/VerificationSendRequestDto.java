package com.han.back.domain.verification.dto.request;

import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.notification.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VerificationSendRequestDto {

    @NotBlank(message = "Target is required.")
    private String target;

    @NotNull(message = "Verification type is required.")
    private VerificationType type;

    @NotNull(message = "Notification channel is required.")
    private NotificationChannel channel;

    public static VerificationSendRequestDto of(
            String target, VerificationType type, NotificationChannel channel) {
        return new VerificationSendRequestDto(target, type, channel);
    }

}