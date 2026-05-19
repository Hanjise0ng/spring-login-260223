package com.han.back.global.util;

public final class RateLimitConst {

    private RateLimitConst() {}

    // ── 인증 코드 ─────────────────────────────────────────────────
    public static final int VERIFY_FAIL_MAX = 5;
    public static final int VERIFY_SEND_HOURLY_MAX = 5;

    // ── 아이디 중복 확인 ──────────────────────────────────────────
    public static final int LOGIN_ID_CHECK_IP_HOURLY_MAX = 30;

    // ── Redis 키 패턴 ─────────────────────────────────────────────
    public static final String VERIFY_FAIL_PREFIX = "rate:verify:fail:";
    public static final String VERIFY_SEND_PREFIX = "rate:verify:send:";
    public static final String LOGIN_ID_CHECK_PREFIX = "rate:check-login-id:ip:";

}