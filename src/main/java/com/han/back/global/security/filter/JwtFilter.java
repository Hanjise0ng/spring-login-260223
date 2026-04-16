package com.han.back.global.security.filter;

import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthHttpUtil;
import com.han.back.global.util.SecurityPathConst;
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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final TokenService tokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return Arrays.stream(SecurityPathConst.PUBLIC_PATHS)
                .anyMatch(pattern -> pathMatcher.match(pattern, request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Optional<String> accessToken = AuthHttpUtil.extractAccessToken(request);

        if (accessToken.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            CustomUserDetails userDetails = tokenService.authenticateAccessToken(accessToken.get());
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()
            );
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("JWT Context Setup - UserPK: {} | SessionId: {} | ClientIP: {}",
                    userDetails.getId(), userDetails.getSessionId(), request.getRemoteAddr());

        } catch (CustomAuthenticationException e) {
            if (isLogoutRequest(request)) { // 로그아웃 요청은 AT 만료·블랙리스트 시에도 통과, LogoutHandler에서 RT 기반 fallback으로 사용자 식별
                log.debug("JWT auth failed on logout path - deferring to LogoutHandler | ClientIP: {}",
                        request.getRemoteAddr());
                filterChain.doFilter(request, response);
                return;
            }
            throw e;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return SecurityPathConst.LOGOUT_PATH.equals(request.getRequestURI());
    }

}