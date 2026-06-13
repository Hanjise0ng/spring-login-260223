package com.han.back.global.security.config;

import com.han.back.global.security.filter.JwtExceptionFilter;
import com.han.back.global.security.filter.JwtFilter;
import com.han.back.global.security.filter.LoginRateLimitFilter;
import com.han.back.global.security.login.LoginFailureRateLimiter;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.util.HttpResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class FilterBeanConfig {

    private final HttpResponseUtil httpResponseUtil;
    private final TokenService tokenService;
    private final LoginFailureRateLimiter loginFailureRateLimiter;
    private final ObjectMapper objectMapper;

    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter(tokenService);
    }

    @Bean
    public JwtExceptionFilter jwtExceptionFilter() {
        return new JwtExceptionFilter(httpResponseUtil);
    }

    @Bean
    public LoginRateLimitFilter loginRateLimitFilter() {
        return new LoginRateLimitFilter(loginFailureRateLimiter, httpResponseUtil, objectMapper);
    }

}