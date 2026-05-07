package com.han.back.global.security.config;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.security.filter.JwtExceptionFilter;
import com.han.back.global.security.filter.JwtFilter;
import com.han.back.global.security.filter.LoginFilter;
import com.han.back.global.security.handler.*;
import com.han.back.global.security.login.CustomLoginFailureHandler;
import com.han.back.global.security.login.CustomLoginSuccessHandler;
import com.han.back.global.security.logout.CustomLogoutHandler;
import com.han.back.global.security.logout.CustomLogoutSuccessHandler;
import com.han.back.global.util.SecurityPathConst;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfig;
    private final AuthenticationConfiguration authConfig;

    private final ObjectMapper objectMapper;

    private final JwtFilter jwtFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final CustomLoginFailureHandler customLoginFailureHandler;
    private final CustomLogoutHandler customLogoutHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    public SecurityConfig(
            @Qualifier("appCorsConfigurationSource") CorsConfigurationSource corsConfig,
            AuthenticationConfiguration authConfig,
            ObjectMapper objectMapper,
            JwtFilter jwtFilter,
            JwtExceptionFilter jwtExceptionFilter,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomLoginSuccessHandler customLoginSuccessHandler,
            CustomLoginFailureHandler customLoginFailureHandler,
            CustomLogoutHandler customLogoutHandler,
            CustomLogoutSuccessHandler customLogoutSuccessHandler
    ) {
        this.corsConfig = corsConfig;
        this.authConfig = authConfig;
        this.objectMapper = objectMapper;
        this.jwtFilter = jwtFilter;
        this.jwtExceptionFilter = jwtExceptionFilter;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customLoginSuccessHandler = customLoginSuccessHandler;
        this.customLoginFailureHandler = customLoginFailureHandler;
        this.customLogoutHandler = customLogoutHandler;
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        configureCors(http);
        configureSecurity(http);
        configureAuthorization(http);
        configureFilters(http, buildLoginFilter(authenticationManager()));
        configureExceptionHandling(http);
        configureLogout(http);

        return http.build();
    }

    private LoginFilter buildLoginFilter(AuthenticationManager authenticationManager) {
        return new LoginFilter(
                authenticationManager,
                objectMapper,
                customLoginSuccessHandler,
                customLoginFailureHandler
        );
    }

    private void configureCors(HttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfig));
    }

    private void configureSecurity(HttpSecurity http) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );
    }

    private void configureAuthorization(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(SecurityPathConst.PUBLIC_PATHS).permitAll()
                .requestMatchers(SecurityPathConst.USER_PATHS).hasAuthority(Role.USER.getAuthority())
                .requestMatchers(SecurityPathConst.ADMIN_PATHS).hasAuthority(Role.ADMIN.getAuthority())
                .anyRequest().authenticated()
        );
    }

    private void configureFilters(HttpSecurity http, LoginFilter loginFilter) {
        http
                .addFilterBefore(jwtFilter, LogoutFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtFilter.class)
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);
    }

    private void configureExceptionHandling(HttpSecurity http) {
        http.exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(customAuthenticationEntryPoint)
        );
    }

    private void configureLogout(HttpSecurity http) {
        http.logout(logout -> logout
                .logoutUrl(SecurityPathConst.LOGOUT_PATH)
                .addLogoutHandler(customLogoutHandler)
                .logoutSuccessHandler(customLogoutSuccessHandler)
        );
    }

}