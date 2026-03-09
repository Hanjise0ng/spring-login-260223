package com.han.back.global.config;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.security.filter.JwtExceptionFilter;
import com.han.back.global.security.filter.JwtFilter;
import com.han.back.global.security.filter.LoginFilter;
import com.han.back.global.security.handler.CustomLogoutHandler;
import com.han.back.global.security.handler.CustomLogoutSuccessHandler;
import com.han.back.global.security.entrypoint.UnauthenticatedEntryPoint;
import com.han.back.global.security.service.TokenService;
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
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final AuthenticationConfiguration authConfig;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

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
        AuthenticationManager authenticationManager = authenticationManager();
        LoginFilter loginFilter = new LoginFilter(authenticationManager, objectMapper, tokenService);
        JwtFilter jwtFilter = new JwtFilter(tokenService);
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
                                "/api/v*/auth/**",
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
                        .authenticationEntryPoint(new UnauthenticatedEntryPoint(objectMapper))
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(new CustomLogoutHandler(objectMapper, tokenService))
                        .logoutSuccessHandler(new CustomLogoutSuccessHandler(objectMapper))
                        .permitAll()
                );

        return http.build();
    }

}