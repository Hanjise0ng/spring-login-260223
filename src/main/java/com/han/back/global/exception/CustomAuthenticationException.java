package com.han.back.global.exception;

import com.han.back.global.dto.BaseResponseStatus;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class CustomAuthenticationException extends AuthenticationException {

    private final BaseResponseStatus status;

    public CustomAuthenticationException(BaseResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }

}