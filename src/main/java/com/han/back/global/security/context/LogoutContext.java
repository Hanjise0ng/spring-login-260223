package com.han.back.global.security.context;

import jakarta.servlet.http.HttpServletRequest;

public final class LogoutContext {

    private LogoutContext() {}

    private static final String ATTR_RESULT = "logout.result";

    public enum Result {
        SUCCESS,
        REDIS_ERROR,
        UNAUTHENTICATED
    }

    public static void setResult(HttpServletRequest request, Result result) {
        request.setAttribute(ATTR_RESULT, result);
    }

    public static Result getResult(HttpServletRequest request) {
        Object result = request.getAttribute(ATTR_RESULT);
        return (result instanceof Result r) ? r : Result.SUCCESS;
    }
}