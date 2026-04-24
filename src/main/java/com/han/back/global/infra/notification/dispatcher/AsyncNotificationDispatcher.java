package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationDispatcher;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.infra.notification.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.dispatcher", havingValue = "async", matchIfMissing = true)
public class AsyncNotificationDispatcher implements NotificationDispatcher {

    private final Map<NotificationChannel, NotificationSender> senderMap;

    public AsyncNotificationDispatcher(List<NotificationSender> senders) {
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::getChannel, Function.identity()));
    }

    @Async("mailExecutor")
    @Override
    public void dispatch(NotificationRequest request) {
        NotificationSender sender = senderMap.get(request.getChannel());
        if (sender == null) {
            log.error("No sender for channel: {} | trace: {}", request.getChannel(), request.getTraceKey());
            return;
        }

        try {
            sender.send(request);
            log.debug("Notification dispatched async - trace: {}", request.getTraceKey());
        } catch (Exception e) {
            log.error("Notification failed async - trace: {} | error: {}",
                    request.getTraceKey(), e.getMessage(), e);
            // throw를 하지 않도록 해서 예외 전파 차단
        }
    }

}