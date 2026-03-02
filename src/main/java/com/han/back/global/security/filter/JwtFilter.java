package com.han.back.global.security.filter;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

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

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return Arrays.stream(EXCLUDE_PATHS)
                .anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = resolveToken(request);

        if (!StringUtils.hasText(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (jwtUtil.isExpired(accessToken)) {
            log.warn("Token Expired - Reason: Access Token Expired | ClientIP: {}", request.getRemoteAddr());
            throw new CustomAuthenticationException(BaseResponseStatus.EXPIRED_JWT_TOKEN);
        }

        if (tokenService.isBlacklisted(accessToken)) {
            log.warn("Blacklist Access Attempt - Reason: Token is Blacklisted | ClientIP: {}", request.getRemoteAddr());
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        Claims claims = jwtUtil.validateAndGetPayload(accessToken);
        if (!AuthConst.TOKEN_TYPE_ACCESS.equals(jwtUtil.getCategory(claims))) {
            log.warn("Invalid Token Category - Reason: Not Access Token | ClientIP: {}", request.getRemoteAddr());
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        Long userId = jwtUtil.getUserId(claims);
        Role role = jwtUtil.getRole(claims);

        CustomUserDetails customUserDetails = new CustomUserDetails(userId, role);
        Authentication authToken = new UsernamePasswordAuthenticationToken(customUserDetails, null, customUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        log.debug("JWT Context Setup - Id: {} | Role: {} | ClientIP: {}",
                userId, role.name(), request.getRemoteAddr());

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}