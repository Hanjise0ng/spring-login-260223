package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationDispatcher;
import com.han.back.global.infra.notification.NotificationIdempotencyGuard;
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
    private final NotificationIdempotencyGuard idempotencyGuard;

    public AsyncNotificationDispatcher(List<NotificationSender> senders,
                                       NotificationIdempotencyGuard idempotencyGuard) {
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::getChannel, Function.identity()));
        this.idempotencyGuard = idempotencyGuard;
    }

    @Async("notificationExecutor")
    @Override
    public void dispatch(NotificationRequest request) {
        long ttlSeconds = selectDedupeTtl(request);
        if (!idempotencyGuard.tryAcquire(request.getDedupeKey(), ttlSeconds)) {
            log.info("Duplicate dispatch skipped - trace: {} | dedupeKey: {}",
                    request.getTraceKey(), request.getDedupeKey());
            return;
        }

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
        }
    }

    private long selectDedupeTtl(NotificationRequest request) {
        return switch (request.getPurpose()) {
            case VERIFICATION -> 3600;
            case WELCOME -> 86400;
            case PASSWORD_RESET -> 3600;
        };
    }

}