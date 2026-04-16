package com.han.back.global.security.login;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;

public interface LoginSuccessProcessor {

    void process(HttpServletRequest request, HttpServletResponse response, Authentication authentication);

}