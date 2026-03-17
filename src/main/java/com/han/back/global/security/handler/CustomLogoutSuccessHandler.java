package com.han.back.global.security.handler;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.security.context.LogoutContext;
import com.han.back.global.security.util.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final HttpResponseUtil httpResponseUtil;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        BaseResponseStatus status = switch (LogoutContext.getResult(request)) {
            case REDIS_ERROR     -> BaseResponseStatus.REDIS_ERROR;
            case UNAUTHENTICATED -> BaseResponseStatus.AUTHENTICATION_FAIL;
            default              -> BaseResponseStatus.SUCCESS;
        };

        httpResponseUtil.writeResponse(response, status);
    }

}