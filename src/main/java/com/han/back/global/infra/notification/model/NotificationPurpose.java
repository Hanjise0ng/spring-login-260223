package com.han.back.global.infra.notification.model;

import java.time.Duration;

public enum NotificationPurpose {

    VERIFICATION(Duration.ofMinutes(1)),
    WELCOME(Duration.ofDays(1)),
    PASSWORD_RESET(Duration.ofMinutes(1)),
    NEW_DEVICE_LOGIN(Duration.ofHours(1));

    private final Duration dedupeTtl;

    NotificationPurpose(Duration dedupeTtl) {
        this.dedupeTtl = dedupeTtl;
    }

    public Duration getDedupeTtl() {
        return dedupeTtl;
    }

}