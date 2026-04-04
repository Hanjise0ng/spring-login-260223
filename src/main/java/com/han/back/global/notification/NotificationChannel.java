package com.han.back.global.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationChannel {

    EMAIL("이메일"),
    SMS("문자메시지");

    private final String displayName;

}