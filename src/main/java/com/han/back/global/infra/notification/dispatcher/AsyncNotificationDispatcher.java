package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.idempotency.IdempotencyGuard;
import com.han.back.global.idempotency.IdempotencyResult;
import com.han.back.global.infra.notification.*;
import com.han.back.global.trace.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnProperty(name = "notification.dispatcher", havingValue = "async", matchIfMissing = true)
public class AsyncNotificationDispatcher implements NotificationDispatcher {

    private static final String SCOPE = "notification";

    private final Map<NotificationChannel, NotificationSender> senderMap;
    private final IdempotencyGuard idempotencyGuard;

    public AsyncNotificationDispatcher(List<NotificationSender> senders,
                                       IdempotencyGuard idempotencyGuard) {
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::getChannel, Function.identity()));
        this.idempotencyGuard = idempotencyGuard;
    }

    @Async("notificationExecutor")
    @Override
    public void dispatch(NotificationCommand command) {
        NotificationRequest request = command.getRequest();
        NotificationMetadata metadata = command.getMetadata();

        if (!TraceContext.isTraced()) {
            log.warn("dispatch() called without trace context - "
                    + "MDC propagation may be broken. "
                    + "traceKey from metadata: {}", metadata.getTraceKey());
        }

        Duration processingTtl = request.getPurpose().getDedupeTtl();

        IdempotencyResult result = idempotencyGuard.tryAcquire(
                SCOPE, metadata.getDedupeKey(), processingTtl);

        if (result != IdempotencyResult.ACQUIRED) {
            log.info("Dispatch skipped [{}] - trace: {} | dedupeKey: {}",
                    result, metadata.getTraceKey(), metadata.getDedupeKey());
            return;
        }

        NotificationSender sender = senderMap.get(request.getChannel());
        if (sender == null) {
            log.error("No sender for channel: {} | trace: {}",
                    request.getChannel(), metadata.getTraceKey());
            idempotencyGuard.release(SCOPE, metadata.getDedupeKey());
            return;
        }

        try {
            sender.send(request);
            idempotencyGuard.complete(SCOPE, metadata.getDedupeKey(),
                    request.getPurpose().getDedupeTtl());
            log.debug("Notification dispatched - trace: {}", metadata.getTraceKey());
        } catch (Exception e) {
            log.error("Notification failed - trace: {} | error: {}",
                    metadata.getTraceKey(), e.getMessage(), e);
            idempotencyGuard.release(SCOPE, metadata.getDedupeKey());
        }
    }

}