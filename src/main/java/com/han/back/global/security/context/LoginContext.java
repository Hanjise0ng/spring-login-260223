package com.han.back.global.security.context;

import jakarta.servlet.http.HttpServletRequest;

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