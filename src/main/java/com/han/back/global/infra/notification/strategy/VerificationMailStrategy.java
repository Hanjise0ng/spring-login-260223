package com.han.back.global.infra.notification.strategy;

import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
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
public class VerificationMailStrategy implements MailSendStrategy {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username:no-reply@han.local}")
    private String fromAddress;

    @Override
    public NotificationPurpose getPurpose() {
        return NotificationPurpose.VERIFICATION;
    }

    @Retryable(
            includes = MailException.class,
            maxRetries = 2,
            delay = 500,
            multiplier = 2,
            maxDelay = 2000
    )
    @Override
    public void send(NotificationRequest request) {
        MailSendUtil.sendMimeMessage(javaMailSender, fromAddress, request);
    }

}