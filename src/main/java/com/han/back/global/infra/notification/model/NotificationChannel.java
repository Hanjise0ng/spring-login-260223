package com.han.back.global.infra.notification.model;

import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationChannel {

    EMAIL("이메일", true),
    SMS("문자메시지", false);

    private final String displayName;
    private final boolean supported;

    public void validateSupported() {
        if (!this.supported) {
            throw new CustomException(VerificationResponseStatus.VERIFY_UNSUPPORTED_CHANNEL);
        }
    }

}