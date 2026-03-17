package com.han.back.global.security.util;

public class AuthConst {

    private AuthConst() {}

    // HTTP 헤더 (Headers)
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_REFRESH_TOKEN_NAME = "Refresh-Token";
    public static final String HEADER_CLIENT_TYPE = "X-Client-Type";
    public static final String HEADER_SET_COOKIE = "Set-Cookie";


    // JWT 페이로드 키 (Claims Keys)
    public static final String TOKEN_TYPE_CATEGORY = "category";
    public static final String TOKEN_USER_PK = "id";
    public static final String TOKEN_ROLE = "role";
    public static final String TOKEN_SESSION_ID = "sid";


    // JWT 토큰 카테고리 값 (Token Types)
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";


    // 임시 토큰 전용 페이로드 키
    public static final String TEMP_USER_EMAIL = "email";
    public static final String TEMP_PROVIDER = "provider";
    public static final String TEMP_PROVIDER_ID = "providerId";


    // 만료 시간 (Expirations - ms 단위 & 초 단위)
    public static final long ACCESS_EXPIRATION = 1800000L;          // JWT 용 (30분)
    public static final long REFRESH_EXPIRATION = 86400000L;        // JWT 용 (24시간)
    public static final int COOKIE_ACCESS_EXPIRATION = 30 * 60;     // 쿠키 용 (30분)
    public static final int COOKIE_REFRESH_EXPIRATION = 24 * 60 * 60; // 쿠키 용 (24시간)


    // 쿠키 설정 (Cookie Configs)
    public static final String COOKIE_REFRESH_TOKEN_NAME = "refresh_token";
    public static final String COOKIE_SAME_SITE = "Strict";


    // Redis 키 접두사 (Redis Prefixes)
    public static final String TOKEN_REFRESH_REDIS_PREFIX = "refresh:";
    public static final String TOKEN_SESSION_BLACKLIST_PREFIX = "blacklist:session:";


}