package com.han.back.global.security.filter;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
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
        } catch (CustomAuthenticationException e) {
            log.warn("JWT Authentication Exception - Code: {} | Message: {} | ClientIP: {}",
                    e.getStatus().getCode(), e.getStatus().getMessage(), request.getRemoteAddr());
            httpResponseUtil.writeResponse(response, e.getStatus());
        } catch (Exception e) {
            log.error("JWT Filter Critical Error - Type: {} | Message: {} | ClientIP: {}",
                    e.getClass().getSimpleName(), e.getMessage(), request.getRemoteAddr(), e);
            httpResponseUtil.writeResponse(response, BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

}