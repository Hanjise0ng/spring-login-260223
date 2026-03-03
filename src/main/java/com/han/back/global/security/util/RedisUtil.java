package com.han.back.global.security.util;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    public RedisUtil(@Qualifier("customStringRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void setDataExpire(String key, String value, long durationMillis) {
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofMillis(durationMillis));
        } catch (Exception e) {
            throw new CustomException(BaseResponseStatus.REDIS_ERROR);
        }
    }

    public boolean setIfAbsent(String key, String value, long durationMillis) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMillis(durationMillis)));
        } catch (Exception e) {
            throw new CustomException(BaseResponseStatus.REDIS_ERROR);
        }
    }

    public String getData(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            throw new CustomException(BaseResponseStatus.REDIS_ERROR);
        }
    }

    public void deleteData(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new CustomException(BaseResponseStatus.REDIS_ERROR);
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            throw new CustomException(BaseResponseStatus.REDIS_ERROR);
        }
    }

}