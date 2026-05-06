package com.han.back.global.infra.notification.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationMetadata {

    private final String traceKey;
    private final String dedupeKey;

    public static NotificationMetadata of(String traceKey, String dedupeKey) {
        return new NotificationMetadata(traceKey, dedupeKey);
    }

}