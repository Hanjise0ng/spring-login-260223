package com.han.back.global.security.util;

public final class SecurityPathConst {

    private SecurityPathConst() {}

    public static final String[] PUBLIC_PATHS = {
            "/",
            "/api/v*/auth/sign-up",
            "/api/v*/auth/sign-in",
            "/api/v*/auth/reissue",
            "/oauth2/**",
            "/login/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

}