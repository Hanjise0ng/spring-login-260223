package com.han.back.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.han.back.domain.auth.dto.request.SignInRequestDto;
import com.han.back.domain.auth.dto.response.SignInResponseDto;
import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    public LoginFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper, JwtUtil jwtUtil) {
        super.setAuthenticationManager(authenticationManager);
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        setFilterProcessesUrl("/api/v1/auth/sign-in");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            SignInRequestDto dto = objectMapper.readValue(request.getInputStream(), SignInRequestDto.class);

            request.setAttribute("attemptedUserId", dto.getUserId());

            log.info("Login Attempt - UserId: {} | ClientIP: {}", dto.getUserId(), request.getRemoteAddr());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            dto.getUserId(),
                            dto.getPassword()
                    );

            return this.getAuthenticationManager().authenticate(authToken);
        } catch (IOException e) {
            throw new CustomAuthenticationException(BaseResponseStatus.INVALID_REQUEST_BODY);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        Long userId = userDetails.getId();
        Role role = userDetails.getRole();

        String accessToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_ACCESS, userId, role, AuthConst.ACCESS_EXPIRATION);
        String refreshToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_REFRESH, userId, role, AuthConst.REFRESH_EXPIRATION);

        response.addHeader("Authorization", "Bearer " + accessToken);

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .path("/api/v1/auth")
                .maxAge(AuthConst.COOKIE_REFRESH_EXPIRATION)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        SignInResponseDto resData = SignInResponseDto.of(accessToken, System.currentTimeMillis() + AuthConst.ACCESS_EXPIRATION);
        setJsonResponse(response, BaseResponseStatus.SUCCESS, resData);

        log.info("Login Success - UserId: {} | Role: {} | ClientIP: {}", userId, role.name(), request.getRemoteAddr());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        BaseResponseStatus clientStatus = BaseResponseStatus.SIGN_IN_FAIL;
        BaseResponseStatus logStatus = BaseResponseStatus.AUTHENTICATION_FAIL;

        String attemptedUserId = (String) request.getAttribute("attemptedUserId");
        if (attemptedUserId == null) attemptedUserId = "UNKNOWN";

        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;

        if (cause instanceof CustomAuthenticationException) {
            clientStatus = ((CustomAuthenticationException) cause).getStatus();
            logStatus = clientStatus;
        }
        else if (cause instanceof UsernameNotFoundException) {
            logStatus = BaseResponseStatus.NOT_FOUND_USER;
        }
        else if (cause instanceof BadCredentialsException) {
            logStatus = BaseResponseStatus.INVALID_PASSWORD;
        }

        log.warn("Login Failed - UserId: {} | LogCode: {} | Reason: {} | ClientIP: {}",
                attemptedUserId,
                logStatus.getCode(),
                logStatus.getMessage(),
                request.getRemoteAddr());

        setJsonResponse(response, clientStatus);
    }

    private void setJsonResponse(HttpServletResponse response, BaseResponseStatus status, Object data) throws IOException {
        response.setStatus(status.getHttpStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Object responseBody;
        if (status == BaseResponseStatus.SUCCESS) {
            responseBody = BaseResponse.success(data).getBody();
        } else {
            responseBody = BaseResponse.error(status).getBody();
        }

        objectMapper.writeValue(response.getWriter(), responseBody);
    }

    private void setJsonResponse(HttpServletResponse response, BaseResponseStatus status) throws IOException {
        setJsonResponse(response, status, null);
    }

}