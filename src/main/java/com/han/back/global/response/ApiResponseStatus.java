package com.han.back.global.response;

import org.springframework.http.HttpStatus;

public interface ApiResponseStatus {

    String getMessage();

    HttpStatus getHttpStatus();

    default String getCode() {
        if (this instanceof Enum<?> e) {
            return e.name();
        }
        throw new IllegalStateException("ApiResponseStatus must be implemented by an enum. Got: " + getClass().getName());
    }

    default int getHttpStatusCode() {
        return getHttpStatus().value();
    }

}