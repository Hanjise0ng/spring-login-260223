package com.han.back.global.config;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.security.entrypoint.UnauthenticatedEntryPoint;
import com.han.back.global.security.filter.JwtExceptionFilter;
import com.han.back.global.security.filter.JwtFilter;
import com.han.back.global.security.filter.LoginFilter;
import com.han.back.global.security.handler.CustomLogoutHandler;
import com.han.back.global.security.handler.CustomLogoutSuccessHandler;
import com.han.back.global.security.service.TokenService;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AuthenticationConfiguration authConfig;
    private final CorsConfigurationSource corsConfigurationSource;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    private final JwtFilter jwtFilter;
    private final JwtExceptionFilter jwtExceptionFilter;

    private final UnauthenticatedEntryPoint unauthenticatedEntryPoint;
    private final CustomLogoutHandler customLogoutHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    public SecurityConfig(
            AuthenticationConfiguration authConfig,
            @Qualifier("appCorsConfigurationSource") CorsConfigurationSource corsConfigurationSource,
            TokenService tokenService,
            ObjectMapper objectMapper,
            JwtFilter jwtFilter,
            JwtExceptionFilter jwtExceptionFilter,
            UnauthenticatedEntryPoint unauthenticatedEntryPoint,
            CustomLogoutHandler customLogoutHandler,
            CustomLogoutSuccessHandler customLogoutSuccessHandler
    ) {
        this.authConfig = authConfig;
        this.corsConfigurationSource = corsConfigurationSource;
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.jwtFilter = jwtFilter;
        this.jwtExceptionFilter = jwtExceptionFilter;
        this.unauthenticatedEntryPoint = unauthenticatedEntryPoint;
        this.customLogoutHandler = customLogoutHandler;
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        LoginFilter loginFilter = new LoginFilter(authenticationManager(), objectMapper, tokenService);

        configureCors(http);
        configureSecurity(http);
        configureAuthorization(http);
        configureFilters(http, loginFilter);
        configureExceptionHandling(http);
        configureLogout(http);

        return http.build();
    }

    private void configureCors(HttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
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
        );
    }

    private void configureFilters(HttpSecurity http, LoginFilter loginFilter) {
        http
                .addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtExceptionFilter, JwtFilter.class);
    }

    private void configureExceptionHandling(HttpSecurity http) {
        http.exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint(unauthenticatedEntryPoint)
        );
    }

    private void configureLogout(HttpSecurity http) {
        http.logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .addLogoutHandler(customLogoutHandler)
                .logoutSuccessHandler(customLogoutSuccessHandler)
                .permitAll()
        );
    }

}