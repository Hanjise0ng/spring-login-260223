package com.han.back.global.security.logout;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.global.response.ApiResponseStatus;
import com.han.back.global.response.ResponseStatus;

public enum LogoutResult {

    SUCCESS(ResponseStatus.SUCCESS),
    REDIS_ERROR(ResponseStatus.REDIS_ERROR),
    UNAUTHENTICATED(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL);

    private final ApiResponseStatus responseStatus;

    LogoutResult(ApiResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public ApiResponseStatus getResponseStatus() {
        return responseStatus;
    }

}