package com.han.back.global.infra.notification;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationRequest {

    private final NotificationChannel channel;
    private final String target;
    private final String subject;
    private final String content;
    private final NotificationPurpose purpose;
    private final LocalDateTime createdAt;

    public static NotificationRequest of(NotificationChannel channel,
                                         String target,
                                         String subject,
                                         String content,
                                         NotificationPurpose purpose) {
        return new NotificationRequest(channel, target, subject, content,
                purpose, LocalDateTime.now());
    }

}