package com.han.back.domain.auth.oauth2.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SocialResponseStatus implements ApiResponseStatus {

    // 400
    SOCIAL_SIGNUP_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "소셜 가입 토큰이 유효하지 않거나 만료되었습니다."),

    // 404
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "소셜 계정을 찾을 수 없습니다."),

    // 409
    SOCIAL_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 연동된 소셜 계정입니다."),
    SOCIAL_EMAIL_CONFLICT(HttpStatus.CONFLICT, "다른 계정에 이미 등록된 이메일입니다."),

    // 422
    SOCIAL_UNSUPPORTED_PROVIDER(HttpStatus.UNPROCESSABLE_CONTENT, "지원하지 않는 소셜 제공자입니다.");

    private final HttpStatus httpStatus;
    private final String message;

}