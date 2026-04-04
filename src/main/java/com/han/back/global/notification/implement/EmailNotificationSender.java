package com.han.back.global.notification.implement;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import com.han.back.global.notification.NotificationChannel;
import com.han.back.global.notification.NotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender javaMailSender;

    @Override
    public void send(String target, String subject, String content) {
        MimeMessage message = createMessage(target, subject, content);
        dispatch(message, target);
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    private MimeMessage createMessage(String target, String subject, String content) {
        MimeMessage message = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(target);
            helper.setSubject(subject);
            helper.setText(content, true);
            return message;
        } catch (MessagingException e) {
            log.error("Email message creation failed: target={}, subject={}", maskEmail(target), subject, e);
            throw new CustomException(BaseResponseStatus.MAIL_FAIL);
        }
    }

    private void dispatch(MimeMessage message, String target) {
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("Email dispatch failed: target={}, cause={}", maskEmail(target), e.getMessage(), e);
            throw new CustomException(BaseResponseStatus.MAIL_FAIL);
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }

}