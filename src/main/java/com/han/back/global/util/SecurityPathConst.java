package com.han.back.global.util;

public final class SecurityPathConst {

    private SecurityPathConst() {}

    public static final String LOGIN_PATH = "/api/v1/auth/sign-in";

    public static final String LOGOUT_PATH = "/api/v1/auth/logout";

    public static final String[] PUBLIC_PATHS = {
            "/",
            "/api/v*/auth/sign-up",
            "/api/v*/auth/sign-in",
            "/api/v*/auth/reissue",
            "/api/v*/auth/check-login-id",
            "/api/v*/auth/oauth2/token",
            "/api/v*/auth/oauth2/complete",
            "/api/v*/verification/**",
            "/oauth2/authorize/**",
            "/login/oauth2/code/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
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