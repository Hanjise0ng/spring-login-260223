package com.han.back.domain.verification.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum VerificationResponseStatus implements ApiResponseStatus {

    // 400
    VERIFY_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않습니다."),
    VERIFY_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증코드가 만료되었거나 무효화되었습니다."),
    VERIFY_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "인증이 완료되지 않았습니다."),

    // 422
    VERIFY_UNSUPPORTED_CHANNEL(HttpStatus.UNPROCESSABLE_CONTENT, "지원하지 않는 알림 채널입니다."),

    // 429
    VERIFY_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 인증코드를 다시 요청해 주세요."),

    // 500
    VERIFY_MAIL_TEMPLATE_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "메일 템플릿 처리에 실패했습니다."),
    VERIFY_MAIL_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다."),
    VERIFY_SMS_SEND_FAIL(HttpStatus.INTERNAL_SERVER_ERROR, "SMS 발송에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String message;

}