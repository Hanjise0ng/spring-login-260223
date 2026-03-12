package com.han.back.global.security.entrypoint;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.security.util.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UnauthenticatedEntryPoint implements AuthenticationEntryPoint {

    private final HttpResponseUtil httpResponseUtil;

    @Override
    public void commence(@NonNull HttpServletRequest request, HttpServletResponse response, @NonNull AuthenticationException authException) {
        httpResponseUtil.writeResponse(response, BaseResponseStatus.NO_PERMISSION);
    }

}