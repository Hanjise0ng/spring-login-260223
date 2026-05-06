package com.han.back.global.security.logout;

import com.han.back.global.response.BaseResponseStatus;

public enum LogoutResult {

    SUCCESS(BaseResponseStatus.SUCCESS),
    REDIS_ERROR(BaseResponseStatus.REDIS_ERROR),
    UNAUTHENTICATED(BaseResponseStatus.AUTHENTICATION_FAIL);

    private final BaseResponseStatus responseStatus;

    LogoutResult(BaseResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }

    public BaseResponseStatus getResponseStatus() {
        return responseStatus;
    }

}