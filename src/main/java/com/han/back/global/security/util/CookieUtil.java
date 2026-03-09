package com.han.back.global.security.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.web.util.WebUtils;

import java.util.Optional;

public class CookieUtil {

    public static Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie cookie = WebUtils.getCookie(request, cookieName);
        return Optional.ofNullable(cookie).map(Cookie::getValue);
    }

    public static void addSecureCookie(HttpServletResponse response, String name, String value, long maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(true)
                .sameSite(AuthConst.COOKIE_SAME_SITE)
                .build();

        response.addHeader(AuthConst.HEADER_SET_COOKIE, cookie.toString());
    }

}