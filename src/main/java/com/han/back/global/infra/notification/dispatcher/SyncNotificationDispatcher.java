package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.idempotency.IdempotencyGuard;
import com.han.back.global.idempotency.IdempotencyResult;
import com.han.back.global.infra.notification.model.NotificationChannel;
import com.han.back.global.infra.notification.model.NotificationCommand;
import com.han.back.global.infra.notification.model.NotificationMetadata;
import com.han.back.global.infra.notification.model.NotificationRequest;
import com.han.back.global.infra.notification.sender.NotificationSender;
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

    private static final String SCOPE = "notification";

    private final Map<NotificationChannel, NotificationSender> senderMap;
    private final IdempotencyGuard idempotencyGuard;

    public SyncNotificationDispatcher(List<NotificationSender> senders,
                                      IdempotencyGuard idempotencyGuard) {
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::getChannel, Function.identity()));
        this.idempotencyGuard = idempotencyGuard;
    }

    @Override
    public void dispatch(NotificationCommand command) {
        NotificationRequest request = command.getRequest();
        NotificationMetadata metadata = command.getMetadata();
        Duration processingTtl = request.getPurpose().getDedupeTtl();

        IdempotencyResult result = idempotencyGuard.tryAcquire(
                SCOPE, metadata.getDedupeKey(), processingTtl);

        if (result != IdempotencyResult.ACQUIRED) {
            log.info("Dispatch skipped (sync) [{}] - trace: {}",
                    result, metadata.getTraceKey());
            return;
        }

        NotificationSender sender = senderMap.get(request.getChannel());
        if (sender == null) {
            idempotencyGuard.release(SCOPE, metadata.getDedupeKey());
            log.error("No sender for channel: {} | trace: {}",
                    request.getChannel(), metadata.getTraceKey());
            throw new IllegalStateException(
                    "No sender for channel: " + request.getChannel());
        }

        try {
            sender.send(request);
            idempotencyGuard.complete(SCOPE, metadata.getDedupeKey(),
                    request.getPurpose().getDedupeTtl());
            log.debug("Notification sent sync - trace: {}", metadata.getTraceKey());
        } catch (Exception e) {
            log.error("Notification failed sync - trace: {} | error: {}",
                    metadata.getTraceKey(), e.getMessage(), e);
            idempotencyGuard.release(SCOPE, metadata.getDedupeKey());
            throw e;
        }
    }

}