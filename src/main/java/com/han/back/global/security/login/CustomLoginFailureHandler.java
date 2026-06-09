package com.han.back.global.security.login;

import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.response.ApiResponseStatus;
import com.han.back.global.response.BaseResponseStatus;
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

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) {
        ApiResponseStatus logStatus = determineLogStatus(exception);
        ApiResponseStatus clientStatus = determineClientStatus(exception);

        String loginId = MDC.get("loginId");
        if (loginId == null) loginId = "UNIDENTIFIED";

        log.warn("Login Failed - LoginId: {} | LogCode: {} | Reason: {} | ClientIP: {}",
                loginId, logStatus.getCode(), logStatus.getMessage(), request.getRemoteAddr());

        httpResponseUtil.writeResponse(response, clientStatus);
    }

    private ApiResponseStatus determineLogStatus(AuthenticationException failed) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;

        if (cause instanceof CustomAuthenticationException e) return e.getStatus();
        if (cause instanceof UsernameNotFoundException) return BaseResponseStatus.NOT_FOUND_USER;
        if (cause instanceof BadCredentialsException) return BaseResponseStatus.INVALID_PASSWORD;
        return BaseResponseStatus.AUTHENTICATION_FAIL;
    }

    private ApiResponseStatus determineClientStatus(AuthenticationException failed) {
        Throwable cause = (failed.getCause() != null) ? failed.getCause() : failed;
        return (cause instanceof CustomAuthenticationException e)
                ? e.getStatus()
                : BaseResponseStatus.SIGN_IN_FAIL;
    }

}