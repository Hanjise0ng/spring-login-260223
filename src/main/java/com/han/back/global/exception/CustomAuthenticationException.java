package com.han.back.global.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class CustomAuthenticationException extends AuthenticationException {

    private final ApiResponseStatus status;

    public CustomAuthenticationException(ApiResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }

}