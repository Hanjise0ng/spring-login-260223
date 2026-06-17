package com.han.back.domain.auth.credential.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CredentialResponseStatus implements ApiResponseStatus {

    // 400
    CREDENTIAL_PROVIDER_NOT_SOCIAL("CREDENTIAL_PROVIDER_NOT_SOCIAL", HttpStatus.BAD_REQUEST, "소셜 제공자가 아닙니다."),

    // 404
    CREDENTIAL_NOT_LINKED("CREDENTIAL_NOT_LINKED", HttpStatus.NOT_FOUND, "연동되지 않은 소셜 계정입니다."),

    // 409
    CREDENTIAL_SOCIAL_ONLY_ACCOUNT("CREDENTIAL_SOCIAL_ONLY_ACCOUNT", HttpStatus.CONFLICT, "소셜 전용 계정은 소셜 연동을 추가하거나 해제할 수 없습니다. 먼저 로컬 계정을 생성해 주세요."),
    CREDENTIAL_PROVIDER_ALREADY_LINKED("CREDENTIAL_PROVIDER_ALREADY_LINKED", HttpStatus.CONFLICT, "이미 동일한 제공자의 소셜 계정이 연동되어 있습니다."),
    CREDENTIAL_SOCIAL_ALREADY_USED("CREDENTIAL_SOCIAL_ALREADY_USED", HttpStatus.CONFLICT, "해당 소셜 계정은 다른 계정에서 이미 사용 중입니다."),
    CREDENTIAL_LOCAL_ALREADY_EXISTS("CREDENTIAL_LOCAL_ALREADY_EXISTS", HttpStatus.CONFLICT, "이미 로컬 계정이 존재합니다.");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

}