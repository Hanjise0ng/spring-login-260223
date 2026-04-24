package com.han.back.global.infra.notification.strategy;

import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.response.BaseResponseStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

@Slf4j
public final class MailSendUtil {

    private MailSendUtil() {}

    public static void sendMimeMessage(JavaMailSender mailSender,
                                       String fromAddress,
                                       NotificationRequest request) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(request.getTarget());
            helper.setSubject(request.getSubject());
            helper.setText(request.getContent(), true);
            mailSender.send(message);
        } catch (MailException e) {
            throw e;
        } catch (MessagingException e) {
            log.error("Mail message assembly failed - trace: {}", request.getTraceKey(), e);
            throw new CustomException(BaseResponseStatus.MAIL_FAIL);
        }
    }

}