package com.han.back.global.security.util;

public final class SecurityPathConst {

    private SecurityPathConst() {}

    public static final String LOGOUT_PATH = "/api/v1/auth/logout";

    public static final String[] PUBLIC_PATHS = {
            "/",
            "/api/v*/auth/sign-up",
            "/api/v*/auth/sign-in",
            "/api/v*/auth/reissue",
            "/api/v*/verification/send",
            "/api/v*/verification/confirm",
            "/oauth2/**",
            "/login/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    public static final String[] USER_PATHS = {
            "/api/v*/user/**",
            "/api/v*/devices/**"
    };

    public static final String[] ADMIN_PATHS = {
            "/api/v*/admin/**"
    };

}