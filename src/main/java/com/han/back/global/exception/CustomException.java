package com.han.back.global.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ApiResponseStatus status;

    public CustomException(ApiResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }

    public CustomException(ApiResponseStatus status, String detailMessage) {
        super(detailMessage);
        this.status = status;
    }

}