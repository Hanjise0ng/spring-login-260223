package com.han.back.global.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BaseResponseStatus {

    // ================== 성공 (200 OK) ==================
    SUCCESS(HttpStatus.OK, "SU", "Success."),


    // ================== 잘못된 요청 (400 Bad Request) ==================
    VALIDATION_FAIL(HttpStatus.BAD_REQUEST, "VF", "Validation failed."),
    CERTIFICATION_FAIL(HttpStatus.BAD_REQUEST, "CF", "Certification failed."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "IRB", "Invalid request body format."),


    // ================== 인증 오류 (401 Unauthorized) ==================
    SIGN_IN_FAIL(HttpStatus.UNAUTHORIZED, "SF", "Login information mismatch."),
    AUTHENTICATION_FAIL(HttpStatus.UNAUTHORIZED, "AUF", "Authentication failed."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "IPW", "Invalid password."),

    // --- JWT Access Token 오류 ---
    INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "IJS", "Invalid JWT signature."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "EJT", "Expired JWT token."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "UJT", "Unsupported JWT token."),
    EMPTY_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "EMT", "JWT token is empty."),


    // --- JWT Refresh Token 오류 ---
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "ERT", "Expired Refresh token. Please log in again."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "IRT", "Invalid Refresh token."),


    // ================== 인가/권한 오류 (403 Forbidden) ==================
    NO_PERMISSION(HttpStatus.FORBIDDEN, "NP", "No Permission."),


    // ================== 자원 없음 (404 Not Found) ==================
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "NFU", "User not found."),
    NOT_FOUND_RESOURCE(HttpStatus.NOT_FOUND, "NFR", "Requested resource not found."),


    // ================== 데이터 충돌 및 논리 오류 (409 Conflict) ==================
    DUPLICATE_ID(HttpStatus.CONFLICT, "DI", "Duplicate Id."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DE", "Duplicate Email."),
    ALREADY_DELETED(HttpStatus.CONFLICT, "AD", "The resource is already deleted."),


    // ================== 서버 내부 오류 (500 Internal Server Error) ==================
    MAIL_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MF", "Mail send failed."),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DBE", "Database error."),
    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "RE", "Redis operation failed."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ISE", "Internal server error.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public int getHttpStatusCode() {
        return httpStatus.value();
    }

}