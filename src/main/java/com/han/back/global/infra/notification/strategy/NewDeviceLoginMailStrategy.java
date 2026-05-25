package com.han.back.global.infra.notification.strategy;

import com.han.back.global.infra.notification.model.NotificationPurpose;
import com.han.back.global.infra.notification.model.NotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewDeviceLoginMailStrategy implements MailSendStrategy {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:no-reply@han.local}")
    private String fromAddress;

    @Override
    public NotificationPurpose getPurpose() {
        return NotificationPurpose.NEW_DEVICE_LOGIN;
    }

    @Retryable(
            includes = MailException.class,
            maxRetries = 3,
            delay = 1000,
            multiplier = 2,
            maxDelay = 10000
    )
    @Override
    public void send(NotificationRequest request) {
        MailSendUtil.sendMimeMessage(javaMailSender, fromAddress, request);
    }

}