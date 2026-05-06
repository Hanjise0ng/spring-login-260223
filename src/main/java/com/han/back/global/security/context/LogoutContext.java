package com.han.back.global.security.context;

import jakarta.servlet.http.HttpServletRequest;

public final class LogoutContext {

    private LogoutContext() {}

    private static final String KEY = "context.logout.result";

    public static void setResult(HttpServletRequest request, LogoutResult result) {
        request.setAttribute(KEY, result);
    }

    public static LogoutResult getResult(HttpServletRequest request) {
        Object result = request.getAttribute(KEY);
        return (result instanceof LogoutResult r) ? r : LogoutResult.UNAUTHENTICATED;
    }

}