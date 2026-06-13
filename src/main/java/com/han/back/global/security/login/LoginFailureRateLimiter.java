package com.han.back.global.security.login;

import com.han.back.global.infra.redis.util.RateLimitUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureRateLimiter {

    private static final int ACCOUNT_LIMIT = 10;
    private static final int IP_LIMIT = 30;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final RateLimitUtil rateLimitUtil;

    public boolean isBlocked(String loginId, String clientIp) {
        return rateLimitUtil.getCount(ipKey(clientIp)) >= IP_LIMIT
                || rateLimitUtil.getCount(accountKey(loginId)) >= ACCOUNT_LIMIT;
    }

    // 인증 실패 — IP 계정 카운터 증가, 반환값은 모니터링 로그로 활용
    public void recordFailure(String loginId, String clientIp) {
        long ipCount = rateLimitUtil.increment(ipKey(clientIp), WINDOW);
        long accountCount = rateLimitUtil.increment(accountKey(loginId), WINDOW);
        log.info("Login failure recorded - account: {}/{} | ip: {}/{}",
                accountCount, ACCOUNT_LIMIT, ipCount, IP_LIMIT);
    }

    public void clearOnSuccess(String loginId) {
        rateLimitUtil.reset(accountKey(loginId));
    }

    private String ipKey(String clientIp) {
        return "login:fail:ip:" + clientIp;
    }

    private String accountKey(String loginId) {
        return "login:fail:account:" + loginId;
    }

}