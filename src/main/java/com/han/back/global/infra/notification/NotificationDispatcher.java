package com.han.back.global.infra.notification;

public interface NotificationDispatcher {

    void dispatch(NotificationRequest request);

}