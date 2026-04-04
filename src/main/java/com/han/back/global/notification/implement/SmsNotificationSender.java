package com.han.back.global.notification.implement;

import com.han.back.global.notification.NotificationChannel;
import com.han.back.global.notification.NotificationSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SmsNotificationSender implements NotificationSender {

    @Override
    public void send(String target, String subject, String content) {

    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.SMS;
    }

}