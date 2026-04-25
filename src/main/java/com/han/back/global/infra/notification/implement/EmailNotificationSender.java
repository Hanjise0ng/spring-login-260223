package com.han.back.global.infra.notification.implement;

import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.infra.notification.NotificationSender;
import com.han.back.global.infra.notification.strategy.MailSendStrategy;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.util.MaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EmailNotificationSender implements NotificationSender {

    private final Map<NotificationPurpose, MailSendStrategy> strategyMap;

    public EmailNotificationSender(List<MailSendStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MailSendStrategy::getPurpose, Function.identity()));

        for (NotificationPurpose purpose : NotificationPurpose.values()) {
            if (!this.strategyMap.containsKey(purpose)) {
                log.warn("No MailSendStrategy for purpose: {}. " +
                        "Mail send for this purpose will fail at runtime.", purpose);
            }
        }
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(NotificationRequest request) {
        MailSendStrategy strategy = strategyMap.get(request.getPurpose());
        if (strategy == null) {
            log.error("No strategy for purpose: {} | target: {} | trace: {}",
                    request.getPurpose(), MaskingUtil.maskTarget(request.getTarget()), request.getTraceKey());
            throw new CustomException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }

        log.debug("Dispatching to {} strategy - target: {} | trace: {}",
                request.getPurpose(), MaskingUtil.maskTarget(request.getTarget()), request.getTraceKey());

        strategy.send(request);
    }

}