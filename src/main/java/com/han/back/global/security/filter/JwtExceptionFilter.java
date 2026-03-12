package com.han.back.global.security.filter;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.util.HttpResponseUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final HttpResponseUtil httpResponseUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CustomAuthenticationException e) { // JWT 검증 실패(만료·위조·블랙리스트 등)
            log.warn("JWT Authentication Exception - Code: {} | Message: {} | ClientIP: {}",
                    e.getStatus().getCode(), e.getStatus().getMessage(), request.getRemoteAddr());
            httpResponseUtil.writeResponse(response, e.getStatus());
        } catch (CustomException e) { // 인프라 오류(Redis 등)
            log.error("Infrastructure error in JWT filter - Code: {} | ClientIP: {}",
                    e.getStatus().getCode(), request.getRemoteAddr());
            httpResponseUtil.writeResponse(response, e.getStatus());
        } catch (Exception e) { // 예상치 못한 런타임 오류 — 필터 체인 전체 범위
            log.error("JWT Filter Critical Error - Type: {} | Message: {} | ClientIP: {}",
                    e.getClass().getSimpleName(), e.getMessage(), request.getRemoteAddr(), e);
            httpResponseUtil.writeResponse(response, BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

}