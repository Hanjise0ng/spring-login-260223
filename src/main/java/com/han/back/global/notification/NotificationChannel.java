package com.han.back.global.notification;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationChannel {

    EMAIL("이메일",true),
    SMS("문자메시지", false);

    private final String displayName;
    private final boolean supported;

    public void validateSupported() {
        if (!this.supported) {
            throw new CustomException(BaseResponseStatus.UNSUPPORTED_NOTIFICATION_CHANNEL);
        }
    }

}