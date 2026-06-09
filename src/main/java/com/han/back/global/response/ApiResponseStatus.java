package com.han.back.global.response;

import org.springframework.http.HttpStatus;

public interface ApiResponseStatus {

    String getCode();

    String getMessage();

    HttpStatus getHttpStatus();

    default int getHttpStatusCode() {
        return getHttpStatus().value();
    }

}