package com.han.back.domain.user.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountResponseStatus implements ApiResponseStatus {

    // 400
    ACCOUNT_SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호가 현재 비밀번호와 동일합니다."),
    ACCOUNT_PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
    ACCOUNT_SOCIAL_ONLY(HttpStatus.BAD_REQUEST, "소셜 전용 계정은 이 작업을 수행할 수 없습니다."),
    ACCOUNT_TAG_GENERATION_FAIL(HttpStatus.BAD_REQUEST, "태그 생성에 실패했습니다. 다시 시도해 주세요."),

    // 401
    ACCOUNT_PASSWORD_RESET_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "비밀번호 재설정 토큰이 유효하지 않거나 만료되었습니다."),

    // 403
    ACCOUNT_STEP_UP_REQUIRED(HttpStatus.FORBIDDEN, "이 작업을 위해 재인증이 필요합니다."),

    // 404
    ACCOUNT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 409
    ACCOUNT_DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    ACCOUNT_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    ACCOUNT_ALREADY_DELETED(HttpStatus.CONFLICT, "이미 탈퇴된 계정입니다."),
    ACCOUNT_NICKNAME_TAG_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 닉네임과 태그 조합입니다.");

    private final HttpStatus httpStatus;
    private final String message;

}