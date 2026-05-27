package com.han.back.domain.auth.oauth2.entity;

import java.time.Duration;

public final class OAuth2Const {

    private OAuth2Const() {}

    // ── one-time code ─────────────────────────────────────────────
    public static final String   OAUTH2_CODE_PREFIX = "oauth2:code:";
    public static final Duration OAUTH2_CODE_TTL = Duration.ofSeconds(120);

    // ── OAuth2 state (CSRF 방지) ──────────────────────────────────
    public static final String   OAUTH2_STATE_PREFIX = "oauth2:state:";
    public static final Duration OAUTH2_STATE_TTL = Duration.ofMinutes(5);

    // ── 소셜 가입 임시 토큰 ───────────────────────────────────────
    public static final Duration SOCIAL_SIGN_UP_TOKEN_TTL = Duration.ofMinutes(10);
    public static final String   TOKEN_CATEGORY_SOCIAL_SIGN_UP = "social_sign_up";

    // ── 소셜 유저 더미값 ──────────────────────────────────────────
    public static final String DUMMY_LOGIN_ID_FORMAT = "%s_%s";
    public static final String DEFAULT_NICKNAME = "social_user";

    // ── OAuth2User attributes 키 ──────────────────────────────────
    public static final String ATTR_USER_INFO = "oauth2UserInfo";
    public static final String ATTR_EXISTING_USER = "existingUserId";

    // ── 프론트 리다이렉트 경로 ────────────────────────────────────
    public static final String FRONT_CALLBACK_PATH = "/callback";
    public static final String FRONT_LOGIN_ERROR_PATH = "/login";

    // ── 프론트 에러 코드 (URL 파라미터용) ─────────────────────────
    public static final String ERROR_EMAIL_CONFLICT = "EMAIL_CONFLICT";
    public static final String ERROR_SOCIAL_LOGIN_FAILED = "SOCIAL_LOGIN_FAILED";

    // ── 태그 ──────────────────────────────────────────────────────
    public static final int TAG_LENGTH = 4;
    public static final int TAG_GENERATION_RETRY = 5;

}