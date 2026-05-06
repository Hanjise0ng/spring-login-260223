package com.han.back.global.idempotency.redis;

import com.han.back.global.idempotency.IdempotencyGuard;
import com.han.back.global.idempotency.IdempotencyResult;
import com.han.back.global.infra.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIdempotencyGuard implements IdempotencyGuard {

    private static final String KEY_PREFIX = "idempotency:";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    private final RedisUtil redisUtil;

    @Override
    public IdempotencyResult tryAcquire(String scope, String key, Duration ttl) {
        if (key == null || key.isBlank()) {
            return IdempotencyResult.ACQUIRED;
        }

        String redisKey = buildKey(scope, key);
        boolean acquired = redisUtil.setIfAbsent(redisKey, STATUS_PROCESSING, ttl);

        if (acquired) {
            return IdempotencyResult.ACQUIRED;
        }

        return redisUtil.getData(redisKey)
                .map(status -> resolveExistingStatus(scope, key, status))
                .orElseGet(() -> retryAcquireAfterTtlExpiry(scope, key, redisKey, ttl));
    }

    @Override
    public void complete(String scope, String key, Duration completeTtl) {
        if (key == null || key.isBlank()) return;
        String redisKey = buildKey(scope, key);
        redisUtil.setDataExpire(redisKey, STATUS_COMPLETED, completeTtl);
        log.debug("Idempotency completed - scope: {} | key: {}", scope, key);
    }

    @Override
    public void release(String scope, String key) {
        if (key == null || key.isBlank()) return;
        String redisKey = buildKey(scope, key);
        redisUtil.deleteData(redisKey);
        log.debug("Idempotency released (retry allowed) - scope: {} | key: {}", scope, key);
    }

    private IdempotencyResult resolveExistingStatus(String scope, String key, String status) {
        if (STATUS_COMPLETED.equals(status)) {
            log.info("Idempotency: already completed - scope: {} | key: {}", scope, key);
            return IdempotencyResult.COMPLETED;
        }
        log.warn("Idempotency: another process is PROCESSING - scope: {} | key: {}", scope, key);
        return IdempotencyResult.PROCESSING;
    }

    private IdempotencyResult retryAcquireAfterTtlExpiry(String scope, String key,
                                                         String redisKey, Duration ttl) {
        boolean retryAcquired = redisUtil.setIfAbsent(redisKey, STATUS_PROCESSING, ttl);
        if (retryAcquired) {
            log.debug("Idempotency: acquired on retry (TTL expired) - scope: {} | key: {}",
                    scope, key);
            return IdempotencyResult.ACQUIRED;
        }
        log.warn("Idempotency: retry SETNX also failed (race condition) - scope: {} | key: {}",
                scope, key);
        return IdempotencyResult.PROCESSING;
    }

    private String buildKey(String scope, String key) {
        return KEY_PREFIX + scope + ":" + key;
    }

}