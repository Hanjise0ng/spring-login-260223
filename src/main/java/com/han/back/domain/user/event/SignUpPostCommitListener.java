package com.han.back.domain.user.event;

import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationDispatcher;
import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.infra.notification.template.MailTemplateUtil;
import com.han.back.global.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignUpPostCommitListener {

    private final VerificationService verificationService;
    private final NotificationDispatcher notificationDispatcher;
    private final MailTemplateUtil mailTemplateUtil;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSignUp(UserSignedUpEvent event) {
        consumeVerificationFlag(event);
        dispatchWelcomeMail(event);
    }

    private void consumeVerificationFlag(UserSignedUpEvent event) {
        try {
            verificationService.consumeConfirmation(event.getEmail(), VerificationType.SIGN_UP);
        } catch (Exception e) {
            log.warn("인증 플래그 소비 실패 (가입은 완료됨) - userId: {} | email: {}",
                    event.getUserId(), MaskingUtil.maskTarget(event.getEmail()), e);
        }
    }

    private void dispatchWelcomeMail(UserSignedUpEvent event) {
        try {
            String subject = String.format("[HAN] %s님, 가입을 환영합니다", event.getNickname());
            String content = mailTemplateUtil.buildWelcomeEmail(
                    event.getNickname(), event.getSignedUpAt());

            NotificationRequest request = NotificationRequest.of(
                    NotificationChannel.EMAIL,
                    event.getEmail(),
                    subject,
                    content,
                    NotificationPurpose.WELCOME,
                    "welcome:user:" + event.getUserId()
            );

            notificationDispatcher.dispatch(request);

            log.info("Welcome mail dispatched - userId: {} | email: {}",
                    event.getUserId(), MaskingUtil.maskTarget(event.getEmail()));
        } catch (Exception e) {
            log.error("Welcome mail dispatch failed - userId: {} | error: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

}