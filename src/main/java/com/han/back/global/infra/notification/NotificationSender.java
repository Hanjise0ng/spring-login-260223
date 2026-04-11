package com.han.back.global.infra.notification;

public interface NotificationSender {

    void send(String target, String subject, String content);

    NotificationChannel getChannel();

}