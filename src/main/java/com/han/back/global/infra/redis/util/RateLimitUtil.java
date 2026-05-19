package com.han.back.global.infra.redis.util;

import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
public class RateLimitUtil {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitUtil(
            @Qualifier("customStringRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public long increment(String key, Duration ttl) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                throw new CustomException(BaseResponseStatus.REDIS_ERROR);
            }
            if (count == 1L) {
                redisTemplate.expire(key, ttl);
            }
            return count;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("RateLimit increment failed - key: {} | error: {}", key, e.getMessage());
            throw new CustomException(BaseResponseStatus.REDIS_ERROR);
        }
    }

    public long incrementHourly(String keyPrefix) {
        String key = keyPrefix + ":" + currentWindow(Duration.ofHours(1));
        return increment(key, Duration.ofHours(1));
    }

    public void reset(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.error("RateLimit reset failed - key: {} | error: {}", key, e.getMessage());
            throw new CustomException(BaseResponseStatus.REDIS_ERROR);
        }
    }

    private String currentWindow(Duration windowSize) {
        long epochSeconds = Instant.now().getEpochSecond();
        long windowSeconds = windowSize.toSeconds();
        long windowStart = (epochSeconds / windowSeconds) * windowSeconds;
        return String.valueOf(windowStart);
    }

}