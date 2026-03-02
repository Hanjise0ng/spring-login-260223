package com.han.back.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.han.back.domain.auth.dto.request.SignInRequestDto;
import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.CookieUtil;
import com.han.back.global.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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
    private final TokenService tokenService;

    public LoginFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper, JwtUtil jwtUtil, TokenService tokenService) {
        super.setAuthenticationManager(authenticationManager);
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        this.tokenService = tokenService;
        setFilterProcessesUrl("/api/v1/auth/sign-in");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            SignInRequestDto dto = objectMapper.readValue(request.getInputStream(), SignInRequestDto.class);
            request.setAttribute("attemptedUserId", dto.getUserId());
            log.info("Login Attempt - UserId: {} | ClientIP: {}", dto.getUserId(), request.getRemoteAddr());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(dto.getUserId(), dto.getPassword());

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

        String oldAccessToken = extractAccessToken(request);
        String oldRefreshToken = extractRefreshToken(request);
        tokenService.invalidatePreviousTokens(oldAccessToken, oldRefreshToken);

        String newAccessToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_ACCESS, userId, role, AuthConst.ACCESS_EXPIRATION);
        String newRefreshToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_REFRESH, userId, role, AuthConst.REFRESH_EXPIRATION);
        tokenService.saveRefreshToken(userId, newRefreshToken);

        response.setHeader("Authorization", "Bearer " + newAccessToken);
        sendRefreshTokenByClientType(request, response, newRefreshToken);

        setJsonResponse(response, BaseResponseStatus.SUCCESS);
        recordSuccessLog(request, userId, role);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        BaseResponseStatus logStatus = determineLogStatus(failed);
        BaseResponseStatus clientStatus = determineClientStatus(failed, logStatus);

        recordFailureLog(request, logStatus);
        setJsonResponse(response, clientStatus);
    }

    // Success Helper Method
    private void sendRefreshTokenByClientType(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        String clientType = request.getHeader("X-Client-Type");
        if ("APP".equalsIgnoreCase(clientType)) {
            response.setHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME, refreshToken);
        } else {
            CookieUtil.addSecureCookie(response, AuthConst.COOKIE_REFRESH_TOKEN_NAME, refreshToken, AuthConst.COOKIE_REFRESH_EXPIRATION);
        }
    }

    private void recordSuccessLog(HttpServletRequest request, Long userId, Role role) {
        String clientType = request.getHeader("X-Client-Type");

        log.info("Login Success - UserId: {} | Role: {} | ClientIP: {} | ClientType: {}",
                userId,
                role.name(),
                request.getRemoteAddr(),
                (clientType != null && !clientType.isBlank() ? clientType : "WEB"));
    }

    // Unsuccess Helper Method
    private BaseResponseStatus determineLogStatus(AuthenticationException failed) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;

        if (cause instanceof CustomAuthenticationException) {
            return ((CustomAuthenticationException) cause).getStatus();
        } else if (cause instanceof UsernameNotFoundException) {
            return BaseResponseStatus.NOT_FOUND_USER;
        } else if (cause instanceof BadCredentialsException) {
            return BaseResponseStatus.INVALID_PASSWORD;
        }
        return BaseResponseStatus.AUTHENTICATION_FAIL;
    }

    private BaseResponseStatus determineClientStatus(AuthenticationException failed, BaseResponseStatus logStatus) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;

        if (cause instanceof CustomAuthenticationException) {
            return logStatus;
        }
        return BaseResponseStatus.SIGN_IN_FAIL;
    }

    private void recordFailureLog(HttpServletRequest request, BaseResponseStatus logStatus) {
        String attemptedUserId = (String) request.getAttribute("attemptedUserId");
        if (attemptedUserId == null) {
            attemptedUserId = "UNKNOWN";
        }

        log.warn("Login Failed - UserId: {} | LogCode: {} | Reason: {} | ClientIP: {}",
                attemptedUserId,
                logStatus.getCode(),
                logStatus.getMessage(),
                request.getRemoteAddr());
    }

    // Common Utility Helper Method
    private void setJsonResponse(HttpServletResponse response, BaseResponseStatus status) throws IOException {
        response.setStatus(status.getHttpStatusCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Object responseBody;
        if (status == BaseResponseStatus.SUCCESS) {
            responseBody = BaseResponse.success().getBody();
        } else {
            responseBody = BaseResponse.error(status).getBody();
        }

        objectMapper.writeValue(response.getWriter(), responseBody);
    }

    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String extractRefreshToken(HttpServletRequest request) {
        String headerToken = request.getHeader(AuthConst.HEADER_REFRESH_TOKEN_NAME);
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken;
        }
        return CookieUtil.getCookieValue(request, AuthConst.COOKIE_REFRESH_TOKEN_NAME).orElse(null);
    }

}