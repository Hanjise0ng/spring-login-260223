package com.han.back.global.infra.notification;

import com.han.back.global.infra.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationIdempotencyGuard {

    private static final String KEY_PREFIX = "notification:dedupe:";

    private final RedisUtil redisUtil;

    public boolean tryAcquire(String dedupeKey, long ttlSeconds) {
        if (dedupeKey == null || dedupeKey.isBlank()) {
            return true;
        }
        String key = KEY_PREFIX + dedupeKey;
        boolean acquired = redisUtil.setIfAbsent(key, "1", ttlSeconds);
        if (!acquired) {
            log.warn("Duplicate notification blocked - dedupeKey: {}", dedupeKey);
        }
        return acquired;
    }

}