package com.han.back.global.security.filter;

import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthHttpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String[] EXCLUDE_PATHS = {
            "/api/*/auth/**",
            "/oauth/**",
            "/login/**",
            "/docs/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final TokenService tokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return Arrays.stream(EXCLUDE_PATHS)
                .anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = AuthHttpUtil.extractAccessToken(request);

        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        CustomUserDetails userDetails = tokenService.authenticateAccessToken(accessToken);

        Authentication authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("JWT Context Setup - Id: {} | Role: {} | ClientIP: {}",
                userDetails.getId(), userDetails.getRole().name(), request.getRemoteAddr());

        filterChain.doFilter(request, response);
    }

}