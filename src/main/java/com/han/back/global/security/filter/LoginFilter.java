package com.han.back.global.security.filter;

import com.han.back.domain.auth.dto.request.SignInRequestDto;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.security.context.LoginContext;
import com.han.back.global.security.login.LoginSuccessProcessor;
import com.han.back.global.util.HttpResponseUtil;
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

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;
    private final LoginSuccessProcessor loginSuccessProcessor;
    private final HttpResponseUtil httpResponseUtil;

    public LoginFilter(AuthenticationManager authenticationManager,
                       ObjectMapper objectMapper,
                       LoginSuccessProcessor loginSuccessProcessor,
                       HttpResponseUtil httpResponseUtil) {
        super.setAuthenticationManager(authenticationManager);
        this.objectMapper = objectMapper;
        this.loginSuccessProcessor = loginSuccessProcessor;
        this.httpResponseUtil = httpResponseUtil;
        setFilterProcessesUrl("/api/v1/auth/sign-in");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
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
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authentication) {
        loginSuccessProcessor.process(request, response, authentication);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                              AuthenticationException failed) {
        BaseResponseStatus logStatus = determineLogStatus(failed);
        BaseResponseStatus clientStatus = determineClientStatus(failed);

        recordFailureLog(request, logStatus);
        httpResponseUtil.writeResponse(response, clientStatus);
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