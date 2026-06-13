package com.han.back.global.security.login;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.response.ApiResponseStatus;
import com.han.back.global.util.ClientIpResolver;
import com.han.back.global.util.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    private final HttpResponseUtil httpResponseUtil;
    private final LoginFailureRateLimiter rateLimiter;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) {
        String loginId = MDC.get("loginId");
        if (loginId == null) loginId = "UNIDENTIFIED";

        rateLimiter.recordFailure(loginId, ClientIpResolver.resolve(request));

        ApiResponseStatus logStatus = determineLogStatus(exception);
        ApiResponseStatus clientStatus = determineClientStatus(exception);
        log.warn("Login Failed - LoginId: {} | LogCode: {} | Reason: {} | ClientIP: {}",
                loginId, logStatus.getCode(), logStatus.getMessage(), ClientIpResolver.resolve(request));

        httpResponseUtil.writeResponse(response, clientStatus);
    }

    private ApiResponseStatus determineLogStatus(AuthenticationException failed) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;

        if (cause instanceof CustomAuthenticationException e) return e.getStatus();
        if (cause instanceof UsernameNotFoundException) return AccountResponseStatus.ACCOUNT_USER_NOT_FOUND;
        if (cause instanceof BadCredentialsException) return AuthResponseStatus.AUTH_INVALID_PASSWORD;
        return AuthResponseStatus.AUTH_AUTHENTICATION_FAIL;
    }

    private ApiResponseStatus determineClientStatus(AuthenticationException failed) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;
        return (cause instanceof CustomAuthenticationException e)
                ? e.getStatus()
                : AuthResponseStatus.AUTH_SIGN_IN_FAIL;
    }

}