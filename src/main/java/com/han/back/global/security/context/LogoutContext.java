package com.han.back.global.security.context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * LogoutHandler → LogoutSuccessHandler 간 처리 결과 전달
 *
 * <pre>
 * CustomLogoutHandler        → set(Result)
 * CustomLogoutSuccessHandler → get(Result) → 응답 코드 결정
 * </pre>
 *
 * <p>미설정 시 기본값: {@link Result#UNAUTHENTICATED}</p>
 *
 * @see com.han.back.global.security.handler.CustomLogoutHandler
 * @see com.han.back.global.security.handler.CustomLogoutSuccessHandler
 */
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
        return (result instanceof Result r) ? r : Result.UNAUTHENTICATED;
    }
}