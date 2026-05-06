package com.han.back.global.infra.notification.sender;

import com.han.back.global.infra.notification.model.NotificationChannel;
import com.han.back.global.infra.notification.model.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsNotificationSender implements NotificationSender {

    @Override
    public void send(NotificationRequest request) {

    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

}