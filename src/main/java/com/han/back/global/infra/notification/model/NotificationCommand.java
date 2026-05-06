package com.han.back.global.infra.notification.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationCommand {

    private final NotificationRequest request;
    private final NotificationMetadata metadata;

    public static NotificationCommand of(NotificationRequest request,
                                         NotificationMetadata metadata) {
        return new NotificationCommand(request, metadata);
    }

}