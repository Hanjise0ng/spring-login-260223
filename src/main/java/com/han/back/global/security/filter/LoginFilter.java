package com.han.back.global.security.filter;

import com.han.back.domain.auth.dto.request.SignInRequestDto;
import com.han.back.domain.user.entity.ClientType;
import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
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
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;
    private final TokenService tokenService;
    private final HttpResponseUtil httpResponseUtil;

    public LoginFilter(AuthenticationManager authenticationManager, ObjectMapper objectMapper, TokenService tokenService, HttpResponseUtil httpResponseUtil) {
        super.setAuthenticationManager(authenticationManager);
        this.objectMapper = objectMapper;
        this.tokenService = tokenService;
        this.httpResponseUtil = httpResponseUtil;
        setFilterProcessesUrl("/api/v1/auth/sign-in");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            SignInRequestDto dto = objectMapper.readValue(request.getInputStream(), SignInRequestDto.class);
            LoginContext.setAttemptedLoginId(request, dto.getLoginId());
            log.info("Login Attempt - LoginId: {} | ClientIP: {}", dto.getLoginId(), request.getRemoteAddr());

            return this.getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getLoginId(), dto.getPassword())
            );
        } catch (IOException e) {
            log.error("Login Request Parsing Error - ClientIP: {}", request.getRemoteAddr(), e);
            throw new CustomAuthenticationException(BaseResponseStatus.INVALID_REQUEST_BODY);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        AuthTokenDto oldTokens = AuthTokenDto.of(
                AuthHttpUtil.extractAccessToken(request).orElse(""),
                AuthHttpUtil.extractRefreshToken(request).orElse("")
        );

        if (!oldTokens.isEmpty()) {
            tokenService.invalidateTokens(oldTokens);
        }

        AuthTokenDto tokenPair = tokenService.issueTokens(userDetails.getId(), userDetails.getRole());
        AuthHttpUtil.setTokenResponse(request, response, tokenPair);
        httpResponseUtil.writeResponse(response, BaseResponseStatus.SUCCESS);

        recordSuccessLog(request, userDetails.getRole());
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException {
        BaseResponseStatus logStatus = determineLogStatus(failed);
        BaseResponseStatus clientStatus = determineClientStatus(failed);

        recordFailureLog(request, logStatus);
        httpResponseUtil.writeResponse(response, clientStatus);
    }

    private void recordSuccessLog(HttpServletRequest request, Role role) {
        String clientType = request.getHeader(AuthConst.HEADER_CLIENT_TYPE);
        String loginId = LoginContext.getAttemptedLoginId(request);

        log.info("Login Success - LoginId: {} | Role: {} | ClientIP: {} | ClientType: {}",
                loginId, role.name(), request.getRemoteAddr(),
                StringUtils.hasText(clientType) ? clientType : ClientType.WEB.name());
    }

    private void recordFailureLog(HttpServletRequest request, BaseResponseStatus logStatus) {
        String loginId = LoginContext.getAttemptedLoginId(request);

        log.warn("Login Failed - LoginId: {} | LogCode: {} | Reason: {} | ClientIP: {}",
                loginId, logStatus.getCode(), logStatus.getMessage(), request.getRemoteAddr());
    }

    private BaseResponseStatus determineLogStatus(AuthenticationException failed) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;

        if (cause instanceof CustomAuthenticationException e) return e.getStatus();
        if (cause instanceof UsernameNotFoundException) return BaseResponseStatus.NOT_FOUND_USER;
        if (cause instanceof BadCredentialsException) return BaseResponseStatus.INVALID_PASSWORD;
        return BaseResponseStatus.AUTHENTICATION_FAIL;
    }

    private BaseResponseStatus determineClientStatus(AuthenticationException failed) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;
        return (cause instanceof CustomAuthenticationException e)
                ? e.getStatus()
                : BaseResponseStatus.SIGN_IN_FAIL;
    }

}