package com.han.back.global.config;

import com.han.back.global.security.filter.JwtExceptionFilter;
import com.han.back.global.security.filter.JwtFilter;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.HttpResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilterBeanConfig {

    private final HttpResponseUtil httpResponseUtil;
    private final TokenService tokenService;

    @Bean
    public JwtFilter jwtFilter() {
        return new JwtFilter(tokenService);
    }

    @Bean
    public JwtExceptionFilter jwtExceptionFilter() {
        return new JwtExceptionFilter(httpResponseUtil);
    }

}