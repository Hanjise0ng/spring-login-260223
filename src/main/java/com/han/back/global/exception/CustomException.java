package com.han.back.global.exception;

import com.han.back.global.dto.BaseResponseStatus;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final BaseResponseStatus status;

    public CustomException(BaseResponseStatus status) {
        super(status.getMessage());
        this.status = status;
    }

    public CustomException(BaseResponseStatus status, String detailMessage) {
        super(detailMessage);
        this.status = status;
    }
}