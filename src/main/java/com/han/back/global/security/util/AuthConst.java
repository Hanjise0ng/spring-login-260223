package com.han.back.global.security.util;

public class AuthConst {

    // 기본 토큰 타입
    public static final String TOKEN_TYPE_CATEGORY = "category";
    public static final String TOKEN_TYPE_ACCESS = "access";
    public static final String TOKEN_TYPE_REFRESH = "refresh";
    public static final String TOKEN_TYPE_TEMP = "temp";


    // 엑세스 토큰, 리프레시 토큰
    public static final String TOKEN_USER_ID = "userId";
    public static final String TOKEN_ROLE = "role";
    public static final long ACCESS_EXPIRATION = 1800000L;
    public static final long REFRESH_EXPIRATION = 86400000L;
    public static final int COOKIE_ACCESS_EXPIRATION = 30 * 60;
    public static final int COOKIE_REFRESH_EXPIRATION = 24 * 60 * 60;


    // Redis 블랙리스트
    public static final String TOKEN_BLACKLIST_PREFIX = "blacklist:";
    public static final String TOKEN_REFRESH_REDIS_PREFIX = "refresh:";


    // 임시 토큰
    public static final String TEMP_USER_EMAIL = "email";
    public static final String TEMP_PROVIDER = "provider";
    public static final String TEMP_PROVIDER_ID = "providerId";

}