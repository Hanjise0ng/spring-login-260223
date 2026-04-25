package com.han.back.global.infra.notification.implement;

import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.infra.notification.NotificationSender;
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