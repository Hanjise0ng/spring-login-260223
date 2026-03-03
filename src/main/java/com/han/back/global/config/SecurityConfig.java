package com.han.back.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.han.back.domain.user.entity.Role;
import com.han.back.global.security.filter.JwtExceptionFilter;
import com.han.back.global.security.filter.JwtFilter;
import com.han.back.global.security.filter.LoginFilter;
import com.han.back.global.security.handler.FailedAuthenticationEntryPoint;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final AuthenticationConfiguration authConfig;
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        ObjectMapper objectMapper = new ObjectMapper();
        AuthenticationManager authenticationManager = authenticationManager();
        LoginFilter loginFilter = new LoginFilter(authenticationManager, objectMapper, tokenService);
        JwtFilter jwtFilter = new JwtFilter(jwtUtil, tokenService);
        JwtExceptionFilter jwtExceptionFilter = new JwtExceptionFilter(objectMapper);

        http
                .cors(cors ->
                        cors.configurationSource(corsConfig)
                )
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(
                                "/",
                                "/api/*/auth/**",
                                "/oauth2/**",
                                "/login/**",

                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers("/api/*/user/**").hasRole(Role.USER.name())
                        .requestMatchers("/api/*/admin/**").hasRole(Role.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtFilter.class)
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(new FailedAuthenticationEntryPoint())
                );

        return http.build();
    }

}