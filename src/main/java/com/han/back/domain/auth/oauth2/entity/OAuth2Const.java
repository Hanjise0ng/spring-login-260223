package com.han.back.domain.auth.oauth2.entity;

import java.time.Duration;

public final class OAuth2Const {

    private OAuth2Const() {}

    // ===== OAuth state (CSRF 방어값, Redis 저장) =====
    public static final Duration OAUTH2_STATE_TTL = Duration.ofMinutes(5);              // state 유효 시간
    public static final String OAUTH2_STATE_PREFIX = "oauth2:state:";                   // state Redis 키 prefix

    // ===== 소셜 가입 임시 토큰 (social_signup_token, 쿠키) =====
    public static final Duration SOCIAL_SIGN_UP_TOKEN_TTL = Duration.ofMinutes(10);      // 가입 토큰 유효 시간
    public static final String TOKEN_CATEGORY_SOCIAL_SIGN_UP = "social_sign_up";         // 가입 토큰 JWT category
    public static final String COOKIE_SOCIAL_SIGNUP_TOKEN_NAME = "social_signup_token";  // 가입 토큰 쿠키명
    public static final String SOCIAL_SIGNUP_COOKIE_PATH = "/api/v1/auth/oauth2";        // 가입 토큰 쿠키 path

    // ===== 소셜 연동 토큰 (social_link, PR 3.7) =====
    public static final String PARAM_LINK_TOKEN = "link_token";                   // 연동 시작 쿼리 파라미터명
    public static final String TOKEN_CATEGORY_SOCIAL_LINK = "social_link";        // 연동 토큰 JWT category
    public static final Duration SOCIAL_LINK_TOKEN_TTL = Duration.ofMinutes(5);   // 연동 토큰 유효 시간
    public static final String CLAIM_LINK_USER_ID = "linkUserId";                 // 연동 토큰 userId claim 키

    // ===== 소셜 연동 컨텍스트 (state → userId, Redis) =====
    public static final String SOCIAL_LINK_CONTEXT_PREFIX = "oauth2:link:";      // 연동 컨텍스트 Redis 키 prefix
    public static final String REGISTRATION_LINK_SUFFIX = "-link";               // 연동 전용 client 등록 접미사
    public static final String PARAM_STATE = "state";                            // OAuth state 파라미터명

    // ===== OAuth2User attribute 키 =====
    public static final String ATTR_USER_INFO = "oauth2UserInfo";            // success handler가 읽는 userInfo 키

    // ===== 더미 계정 / 기본값 =====
    public static final String DUMMY_LOGIN_ID_FORMAT = "%s_%s";              // 소셜 더미 로그인ID 포맷
    public static final String DEFAULT_NICKNAME = "social_user";             // 닉네임 미제공 시 기본값

    // ===== 프론트 리다이렉트 경로 =====
    public static final String FRONT_CALLBACK_PATH = "/callback";                    // 로그인/가입 결과 페이지
    public static final String FRONT_LOGIN_ERROR_PATH = "/login";                    // 로그인 실패 페이지
    public static final String FRONT_LINK_COMPLETE_PATH = "/mypage/social/callback"; // 연동 결과 페이지

    // ===== 리다이렉트 파라미터 / 상태값 =====
    public static final String PARAM_STATUS = "status";                            // 결과 상태 파라미터명
    public static final String PARAM_ERROR = "error";                              // 에러 코드 파라미터명
    public static final String STATUS_EMAIL_REQUIRED = "email_required";           // 이메일 미제공(가입)
    public static final String STATUS_LINK_SUGGESTED = "link_suggested";           // 이메일 충돌(연동/별도가입 선택)
    public static final String STATUS_LINK_SUCCESS = "link_success";               // 연동 성공
    public static final String ERROR_SOCIAL_LOGIN_FAILED = "SOCIAL_LOGIN_FAILED";  // 소셜 로그인 실패

    // ===== 태그 생성 =====
    public static final int TAG_LENGTH = 4;                     // 사용자 태그 길이
    public static final int TAG_GENERATION_RETRY = 5;           // 태그 중복 재시도 횟수

}