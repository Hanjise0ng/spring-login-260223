package com.han.back.global.security.filter;

import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.dto.Empty;
import com.han.back.global.exception.CustomAuthenticationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtExceptionFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (CustomAuthenticationException e) {
            log.warn("JWT Authentication Exception - Code: {} | Message: {} | ClientIP: {}",
                    e.getStatus().getCode(), e.getStatus().getMessage(), request.getRemoteAddr());
            setErrorResponse(response, e.getStatus());
        } catch (Exception e) {
            log.error("JWT Filter Critical Error - Type: {} | Message: {} | ClientIP: {}",
                    e.getClass().getSimpleName(), e.getMessage(), request.getRemoteAddr(), e);
            setErrorResponse(response, BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void setErrorResponse(HttpServletResponse response, BaseResponseStatus status) throws IOException {
        response.setStatus(status.getHttpStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        BaseResponse<Empty> errorBody = BaseResponse.error(status).getBody();
        objectMapper.writeValue(response.getWriter(), errorBody);
    }

}