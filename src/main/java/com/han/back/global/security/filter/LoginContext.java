package com.han.back.global.security.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * LoginFilter 내 메서드 간 부가 데이터 전달 수단.
 *
 * attemptAuthentication()에서 파싱한 loginId를
 * successfulAuthentication()/unsuccessfulAuthentication()의
 * 로깅에서 사용하기 위해 request scope에 보관.
 *
 * request attribute를 직접 사용하는 것 대비 이점:
 * - 키 하드코딩 제거
 * - 타입 캐스팅 캡슐화
 */
public final class LoginContext {

    private LoginContext() {}

    private static final String ATTR_LOGIN_ID = "login.attemptedLoginId";

    public static void setAttemptedLoginId(HttpServletRequest request, String loginId) {
        request.setAttribute(ATTR_LOGIN_ID, loginId);
    }

    public static String getAttemptedLoginId(HttpServletRequest request) {
        Object loginId = request.getAttribute(ATTR_LOGIN_ID);
        return (loginId instanceof String s) ? s : "UNIDENTIFIED";
    }
}