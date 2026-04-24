package com.han.back.global.infra.notification.strategy;

import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;

public interface MailSendStrategy {

    NotificationPurpose getPurpose();

    void send(NotificationRequest request);

}