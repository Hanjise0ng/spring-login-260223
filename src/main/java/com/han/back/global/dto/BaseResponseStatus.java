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
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "SCP", "New password must be different from current password."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "PCM", "Password confirmation does not match."),
    VERIFICATION_EXPIRED(HttpStatus.BAD_REQUEST, "VE", "Verification code has expired."),
    VERIFICATION_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "VNC", "Verification has not been completed."),
    SOCIAL_ONLY_ACCOUNT(HttpStatus.BAD_REQUEST, "SOA", "Social-only accounts cannot perform this action."),
    SELF_DEVICE_FORCE_LOGOUT(HttpStatus.BAD_REQUEST, "SDL", "Cannot force logout current device. Use normal logout."),


    // ================== 인증 오류 (401 Unauthorized) ==================
    SIGN_IN_FAIL(HttpStatus.UNAUTHORIZED, "SF", "Login information mismatch."),
    AUTHENTICATION_FAIL(HttpStatus.UNAUTHORIZED, "AUF", "Authentication failed."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "IPW", "Invalid password."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "PRI", "Password reset token is invalid or expired."),

    // --- JWT Access Token 오류 ---
    INVALID_JWT_SIGNATURE(HttpStatus.UNAUTHORIZED, "IJS", "Invalid JWT signature."),
    EXPIRED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "EJT", "Expired JWT token."),
    UNSUPPORTED_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "UJT", "Unsupported JWT token."),
    EMPTY_JWT_TOKEN(HttpStatus.UNAUTHORIZED, "EMT", "JWT token is empty."),
    MISSING_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "MAT", "Access token is missing."),
    MISSING_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "MRT", "Refresh token is missing."),

    // --- JWT Refresh Token 오류 ---
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "ERT", "Expired Refresh token. Please log in again."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "IRT", "Invalid Refresh token."),


    // ================== 인가/권한 오류 (403 Forbidden) ==================
    NO_PERMISSION(HttpStatus.FORBIDDEN, "NP", "No Permission."),
    DEVICE_BANNED(HttpStatus.FORBIDDEN, "DB", "This device has been blocked."),
    STEP_UP_REQUIRED(HttpStatus.FORBIDDEN, "SUR", "Re-authentication is required for this action."),


    // ================== 자원 없음 (404 Not Found) ==================
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "NFU", "User not found."),
    NOT_FOUND_RESOURCE(HttpStatus.NOT_FOUND, "NFR", "Requested resource not found."),
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "SNF", "Social account not found."),
    NOT_FOUND_DEVICE(HttpStatus.NOT_FOUND, "NFD", "Device not found."),


    // ================== 데이터 충돌 및 논리 오류 (409 Conflict) ==================
    DUPLICATE_ID(HttpStatus.CONFLICT, "DI", "Duplicate Id."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DE", "Duplicate Email."),
    ALREADY_DELETED(HttpStatus.CONFLICT, "AD", "The resource is already deleted."),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "SAL", "This social account is already linked."),


    // ================== 요청 처리 불가 (422 Unprocessable Content) ==================
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.UNPROCESSABLE_CONTENT, "USP", "Unsupported social provider."),


    // ================== 요청 과다 (429 Too Many Requests) ==================
    COOLDOWN_ACTIVE(HttpStatus.TOO_MANY_REQUESTS, "CA", "Please wait before requesting a new code."),


    // ================== 서버 내부 오류 (500 Internal Server Error) ==================
    MAIL_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "MF", "Mail send failed."),
    SMS_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SSF", "SMS send failed."),
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