package com.han.back.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResponseStatus implements ApiResponseStatus {

    // 200
    SUCCESS(HttpStatus.OK, "성공"),

    // 400
    VALIDATION_FAIL(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),
    MALFORMED_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다."),

    // 403
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "대상 리소스를 찾을 수 없습니다."),

    // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 HTTP 메서드입니다."),

    // 415
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),

    // 429
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),

    // 500
    DB_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DB 처리 중 오류가 발생했습니다."),
    REDIS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Redis 처리 중 오류가 발생했습니다."),
    SERIALIZATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "직렬화 중 오류가 발생했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

}