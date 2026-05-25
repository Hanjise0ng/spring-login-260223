package com.han.back.domain.device.event;

import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.infra.notification.dispatcher.NotificationDispatcher;
import com.han.back.global.infra.notification.model.*;
import com.han.back.global.infra.notification.policy.NotificationKeyPolicy;
import com.han.back.global.infra.notification.template.MailTemplateUtil;
import com.han.back.global.trace.TraceContext;
import com.han.back.global.util.MaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewDeviceLoginEventHandler {

    private final NotificationDispatcher notificationDispatcher;
    private final MailTemplateUtil mailTemplateUtil;
    private final NotificationKeyPolicy keyPolicy;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewDeviceLogin(NewDeviceLoginEvent event) {
        dispatchNewDeviceLoginMail(event);
    }

    private void dispatchNewDeviceLoginMail(NewDeviceLoginEvent event) {
        try {
            String subject = String.format("[HAN] %s님, 새로운 기기에서 로그인되었습니다",
                    event.getNickname());
            String content = mailTemplateUtil.buildNewDeviceLoginEmail(
                    event.getNickname(),
                    event.getDeviceType(),
                    event.getOsName(),
                    event.getLoginIp(),
                    event.getLoginAt()
            );

            notificationDispatcher.dispatch(NotificationCommand.of(
                    NotificationRequest.of(
                            NotificationChannel.EMAIL,
                            event.getEmail(),
                            subject,
                            content,
                            NotificationPurpose.NEW_DEVICE_LOGIN
                    ),
                    NotificationMetadata.of(
                            TraceContext.getTraceId(),
                            keyPolicy.newDeviceLogin(event.getUserId(),
                                    event.getDeviceFingerprint())
                    )
            ));

            log.info("New device login mail dispatched - userId: {} | email: {}",
                    event.getUserId(), MaskingUtil.maskTarget(event.getEmail()));

        } catch (Exception e) {
            log.error("New device login mail dispatch failed - userId: {} | error: {}",
                    event.getUserId(), e.getMessage(), e);
        }
    }

}