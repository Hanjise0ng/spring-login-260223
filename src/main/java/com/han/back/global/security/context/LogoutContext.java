package com.han.back.global.security.context;

import com.han.back.global.response.BaseResponseStatus;
import jakarta.servlet.http.HttpServletRequest;

public final class LogoutContext {

    private LogoutContext() {}

    private static final String ATTR_RESULT = "logout.result";

    public enum Result {

        SUCCESS(BaseResponseStatus.SUCCESS),
        REDIS_ERROR(BaseResponseStatus.REDIS_ERROR),
        UNAUTHENTICATED(BaseResponseStatus.AUTHENTICATION_FAIL);

        private final BaseResponseStatus responseStatus;

        Result(BaseResponseStatus responseStatus) {
            this.responseStatus = responseStatus;
        }

        public BaseResponseStatus getResponseStatus() {
            return responseStatus;
        }
    }

    public static void setResult(HttpServletRequest request, Result result) {
        request.setAttribute(ATTR_RESULT, result);
    }

    public static Result getResult(HttpServletRequest request) {
        Object result = request.getAttribute(ATTR_RESULT);
        return (result instanceof Result r) ? r : Result.UNAUTHENTICATED;
    }

}