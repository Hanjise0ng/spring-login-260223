package com.han.back.global.security.service;

import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import com.han.back.global.security.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;

    public void saveRefreshToken(Long userId, String rt) {
        long ttl = jwtUtil.getExpiration(rt);
        redisUtil.setDataExpire(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userId, rt, ttl);
    }

    public String getRefreshToken(Long userId) {
        return redisUtil.getData(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userId);
    }

    public void deleteRefreshToken(Long userId) {
        redisUtil.deleteData(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userId);
    }

    public void addToBlacklist(String at) {
        if (jwtUtil.isExpired(at)) {
            log.info("Blacklist Skipped - Reason: Token already expired or invalid");
            return;
        }

        long ttl = jwtUtil.getExpiration(at);
        redisUtil.setDataExpire(AuthConst.TOKEN_BLACKLIST_PREFIX + at, "logout", ttl);

        log.info("Access Token Blacklisted - TTL: {}", ttl);
    }

    public boolean isBlacklisted(String at) {
        return redisUtil.hasKey(AuthConst.TOKEN_BLACKLIST_PREFIX + at);
    }

    public void invalidatePreviousTokens(String oldAccessToken, String oldRefreshToken) {
        if (oldAccessToken != null && !oldAccessToken.isBlank()) {
            addToBlacklist(oldAccessToken);
        }

        if (oldRefreshToken != null && !oldRefreshToken.isBlank()) {
            if (jwtUtil.isExpired(oldRefreshToken)) {
                log.info("Refresh Token Deletion Skipped - Reason: Already expired or invalid");
                return;
            }

            Long userId = jwtUtil.getUserId(oldRefreshToken);
            deleteRefreshToken(userId);

            log.info("Refresh Token Deleted - UserId: {}", userId);
        }
    }

}