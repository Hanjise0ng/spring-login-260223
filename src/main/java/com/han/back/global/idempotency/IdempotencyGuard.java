package com.han.back.global.idempotency;

import java.time.Duration;

public interface IdempotencyGuard {

    IdempotencyResult tryAcquire(String scope, String key, Duration ttl);

    void complete(String scope, String key, Duration completeTtl);

    void release(String scope, String key);

}