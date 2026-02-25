package com.han.back.global.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BaseResponseStatus {

    // ================== 성공 (Success) ==================
    SUCCESS(HttpStatus.OK, "SU", "Success."),


    // ================== 4xx 클라이언트 오류 ==================
    // 유효성 및 요청 본문 오류 (400 Bad Request)
    VALIDATION_FAIL(HttpStatus.BAD_REQUEST, "VF", "Validation failed."),
    CERTIFICATION_FAIL(HttpStatus.BAD_REQUEST, "CF", "Certification failed."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "IRB", "Invalid request body format."),


    // 인증/인가 오류 (401 Unauthorized / 403 Forbidden / 409 Conflict)
    DUPLICATE_ID(HttpStatus.CONFLICT, "DI", "Duplicate Id."),
    SIGN_IN_FAIL(HttpStatus.UNAUTHORIZED, "SF", "Login information mismatch."),
    NO_PERMISSION(HttpStatus.FORBIDDEN, "NP", "No Permission."),
    AUTHENTICATION_FAIL(HttpStatus.UNAUTHORIZED, "AUF", "Authentication failed."),


    // ================== 5xx 서버 오류 ==================
    MAIL_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MF", "Mail send failed."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DBE", "Database error."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ISE", "Internal server error.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

}