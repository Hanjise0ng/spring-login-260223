package com.han.back.global.infra.notification.sender;

import com.han.back.global.infra.notification.model.NotificationChannel;
import com.han.back.global.infra.notification.model.NotificationRequest;

public interface NotificationSender {

    void send(NotificationRequest request);

    NotificationChannel getChannel();

}