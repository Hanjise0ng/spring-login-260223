package com.han.back.global.security.token;

import java.time.Duration;

public final class AuthConst {

    private AuthConst() {}

    // ── HTTP 헤더 ──────────────────────────────────────────────────
    public static final String BEARER_PREFIX            = "Bearer ";
    public static final String HEADER_REFRESH_TOKEN_NAME = "Refresh-Token";
    public static final String HEADER_CLIENT_TYPE       = "X-Client-Type";
    public static final String HEADER_SET_COOKIE        = "Set-Cookie";

    // ── JWT Claims 키 ──────────────────────────────────────────────
    public static final String TOKEN_TYPE_CATEGORY = "category";
    public static final String TOKEN_USER_PK       = "id";
    public static final String TOKEN_ROLE          = "role";
    public static final String TOKEN_SESSION_ID    = "sid";

    // ── JWT 토큰 카테고리 값 ───────────────────────────────────────
    public static final String TOKEN_TYPE_ACCESS  = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";

    // ── 임시 토큰 전용 Claims 키 ──────────────────────────────────
    public static final String TEMP_USER_EMAIL  = "email";
    public static final String TEMP_PROVIDER    = "provider";
    public static final String TEMP_PROVIDER_ID = "providerId";

    // ── 만료 시간 (Duration — 단일 진실의 원천) ───────────────────
    // JWT, Redis, 쿠키 모두 이 Duration 에서 필요한 단위를 추출해서 사용
    public static final Duration ACCESS_TOKEN_TTL     = Duration.ofMinutes(30);
    public static final Duration REFRESH_TOKEN_TTL    = Duration.ofDays(1);
    public static final Duration LOGIN_ID_TOKEN_TTL   = Duration.ofMinutes(30);

    // ── 쿠키 설정 ─────────────────────────────────────────────────
    public static final String COOKIE_REFRESH_TOKEN_NAME = "refresh_token";
    public static final String COOKIE_SAME_SITE          = "Strict";

    // ── Redis 키 접두사 ───────────────────────────────────────────
    public static final String TOKEN_REFRESH_REDIS_PREFIX      = "refresh:";
    public static final String TOKEN_SESSION_BLACKLIST_PREFIX  = "blacklist:session:";

    // ── 디바이스 헤더 ─────────────────────────────────────────────
    public static final String HEADER_DEVICE_ID = "X-Device-Id";
    public static final String HEADER_DEVICE_OS = "X-Device-Os";
    public static final String CLIENT_TYPE_APP  = "APP";

    // ── 디바이스 쿠키 ─────────────────────────────────────────────
    public static final String  COOKIE_DEVICE_ID_NAME = "device_id";
    public static final Duration DEVICE_COOKIE_TTL    = Duration.ofDays(365);

}