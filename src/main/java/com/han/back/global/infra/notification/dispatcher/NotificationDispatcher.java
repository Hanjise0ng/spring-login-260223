package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.infra.notification.model.NotificationCommand;

public interface NotificationDispatcher {

    void dispatch(NotificationCommand command);

}