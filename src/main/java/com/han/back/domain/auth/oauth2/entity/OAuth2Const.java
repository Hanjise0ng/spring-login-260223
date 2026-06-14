package com.han.back.domain.auth.oauth2.entity;

import java.time.Duration;

public final class OAuth2Const {

    private OAuth2Const() {}

    public static final Duration OAUTH2_STATE_TTL = Duration.ofMinutes(5);
    public static final String   OAUTH2_STATE_PREFIX = "oauth2:state:";

    public static final Duration SOCIAL_SIGN_UP_TOKEN_TTL = Duration.ofMinutes(10);
    public static final String   TOKEN_CATEGORY_SOCIAL_SIGN_UP = "social_sign_up";

    public static final String COOKIE_SOCIAL_SIGNUP_TOKEN_NAME = "social_signup_token";
    public static final String SOCIAL_SIGNUP_COOKIE_PATH = "/api/v1/auth/oauth2";

    public static final String DUMMY_LOGIN_ID_FORMAT = "%s_%s";
    public static final String DEFAULT_NICKNAME = "social_user";

    public static final String ATTR_USER_INFO = "oauth2UserInfo";

    public static final String FRONT_CALLBACK_PATH = "/callback";
    public static final String FRONT_LOGIN_ERROR_PATH = "/login";

    public static final String PARAM_STATUS = "status";
    public static final String STATUS_EMAIL_REQUIRED = "email_required";
    public static final String STATUS_LINK_SUGGESTED = "link_suggested";
    public static final String ERROR_SOCIAL_LOGIN_FAILED = "SOCIAL_LOGIN_FAILED";

    public static final int TAG_LENGTH = 4;
    public static final int TAG_GENERATION_RETRY = 5;

}