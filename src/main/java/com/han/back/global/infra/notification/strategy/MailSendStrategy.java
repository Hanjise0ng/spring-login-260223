package com.han.back.global.infra.notification.strategy;

import com.han.back.global.infra.notification.model.NotificationPurpose;
import com.han.back.global.infra.notification.model.NotificationRequest;

public interface MailSendStrategy {

    NotificationPurpose getPurpose();

    void send(NotificationRequest request);

}