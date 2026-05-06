package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.infra.notification.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.dispatcher", havingValue = "sync")
public class SyncNotificationDispatcher implements NotificationDispatcher {

    private final Map<NotificationChannel, NotificationSender> senderMap;
    private final NotificationIdempotencyGuard idempotencyGuard;

    public SyncNotificationDispatcher(List<NotificationSender> senders,
                                      NotificationIdempotencyGuard idempotencyGuard) {
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::getChannel, Function.identity()));
        this.idempotencyGuard = idempotencyGuard;
    }

    public void dispatch(NotificationRequest request) {
        Duration ttl = request.getPurpose().getDedupeTtl();

        if (!idempotencyGuard.tryAcquire(request.getDedupeKey(), ttl)) {
            log.info("Duplicate dispatch skipped (sync) - trace: {}", request.getTraceKey());
            return;
        }

        NotificationSender sender = senderMap.get(request.getChannel());
        if (sender == null) {
            log.error("No sender for channel: {} | trace: {}", request.getChannel(), request.getTraceKey());
            throw new IllegalStateException("No sender for channel: " + request.getChannel());
        }

        try {
            sender.send(request);
            log.info("Notification sent sync - trace: {}", request.getTraceKey());
        } catch (Exception e) {
            log.error("Notification failed sync - trace: {} | error: {}",
                    request.getTraceKey(), e.getMessage(), e);
            throw e;
        }
    }

}