package com.han.back.domain.auth.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthResponseStatus implements ApiResponseStatus {

    // 400
    AUTH_LOGIN_ID_CHECK_REQUIRED(HttpStatus.BAD_REQUEST, "회원가입 전 아이디 중복 확인이 필요합니다."),

    // 401
    AUTH_SIGN_IN_FAIL(HttpStatus.UNAUTHORIZED, "로그인 정보가 일치하지 않습니다."),
    AUTH_AUTHENTICATION_FAIL(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    AUTH_INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
    AUTH_INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "JWT 서명이 올바르지 않습니다."),
    AUTH_EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Access Token이 만료되었습니다."),
    AUTH_UNSUPPORTED_JWT(HttpStatus.UNAUTHORIZED, "지원하지 않는 JWT입니다."),
    AUTH_EMPTY_JWT(HttpStatus.UNAUTHORIZED, "JWT가 비어 있습니다."),
    AUTH_MISSING_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "Access Token이 누락되었습니다."),
    AUTH_MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 누락되었습니다."),
    AUTH_EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다. 다시 로그인해 주세요."),
    AUTH_INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String message;

}