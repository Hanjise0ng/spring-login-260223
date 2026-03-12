package com.han.back.global.security.handler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * LogoutHandler → LogoutSuccessHandler 간 처리 결과 전달 수단.
 *
 * Spring Security LogoutFilter 계약상 두 핸들러 사이에
 * 데이터를 전달할 공식 채널이 없으므로 request scope을 활용.
 *
 * request attribute를 직접 사용하는 것 대비 이점:
 * - 키 하드코딩 제거
 * - 타입 캐스팅 캡슐화
 * - 전달 가능한 값을 Result enum으로 명시적 제한
 */
public final class LogoutContext {

    private LogoutContext() {}

    private static final String ATTR_RESULT = "logout.result";

    public enum Result {
        SUCCESS,
        REDIS_ERROR
    }

    public static void setResult(HttpServletRequest request, Result result) {
        request.setAttribute(ATTR_RESULT, result);
    }

    public static Result getResult(HttpServletRequest request) {
        Object result = request.getAttribute(ATTR_RESULT);
        return (result instanceof Result r) ? r : Result.SUCCESS;
    }
}