package com.han.back.global.infra.notification;

public interface NotificationSender {

    void send(NotificationRequest request);

    NotificationChannel getChannel();

}