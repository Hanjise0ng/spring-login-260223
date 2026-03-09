package com.han.back.global.security.filter;

import com.han.back.domain.auth.dto.request.SignInRequestDto;
import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthHttpUtil;
import com.han.back.global.security.util.HttpResponseUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Optional;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    public LoginFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper, TokenService tokenService) {
        super.setAuthenticationManager(authenticationManager);
        this.objectMapper = objectMapper;
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
            log.error("Login Request Parsing Error - ClientIP: {}", request.getRemoteAddr(), e);
            throw new CustomAuthenticationException(BaseResponseStatus.INVALID_REQUEST_BODY);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long id = userDetails.getId();
        Role role = userDetails.getRole();

        AuthTokenDto oldTokens = AuthTokenDto.of(
                AuthHttpUtil.extractAccessToken(request).orElse(""),
                AuthHttpUtil.extractRefreshToken(request).orElse("")
        );

        if (!oldTokens.isEmpty()) {
            tokenService.invalidateTokens(oldTokens);
        }

        AuthTokenDto tokenPair = tokenService.issueTokens(id, role);
        AuthHttpUtil.setTokenResponse(request, response, tokenPair);

        HttpResponseUtil.writeResponse(response, objectMapper, BaseResponseStatus.SUCCESS);
        recordSuccessLog(request, id, role);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        BaseResponseStatus logStatus = determineLogStatus(failed);
        BaseResponseStatus clientStatus = determineClientStatus(failed, logStatus);

        recordFailureLog(request, logStatus);
        HttpResponseUtil.writeResponse(response, objectMapper, clientStatus);
    }

    private void recordSuccessLog(HttpServletRequest request, Long id, Role role) {
        String clientType = request.getHeader("X-Client-Type");

        log.info("Login Success - UserId: {} | Role: {} | ClientIP: {} | ClientType: {}",
                id, role.name(), request.getRemoteAddr(),
                (clientType != null && !clientType.isBlank() ? clientType : "WEB"));
    }

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
        String attemptedUserId = Optional.ofNullable(
                (String) request.getAttribute("attemptedUserId")
        ).orElse("UNIDENTIFIED");

        log.warn("Login Failed - UserId: {} | LogCode: {} | Reason: {} | ClientIP: {}",
                attemptedUserId, logStatus.getCode(), logStatus.getMessage(), request.getRemoteAddr());
    }

}