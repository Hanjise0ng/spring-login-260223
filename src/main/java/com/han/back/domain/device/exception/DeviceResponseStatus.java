package com.han.back.domain.device.exception;

import com.han.back.global.response.ApiResponseStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DeviceResponseStatus implements ApiResponseStatus {

    // 400
    DEVICE_SELF_LOGOUT_FORBIDDEN(HttpStatus.BAD_REQUEST, "현재 디바이스는 강제 로그아웃할 수 없습니다. 일반 로그아웃을 사용하세요."),

    // 403
    DEVICE_BANNED(HttpStatus.FORBIDDEN, "차단된 디바이스입니다."),

    // 404
    DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "디바이스를 찾을 수 없습니다."),

    // 409
    DEVICE_ACTIVE_DELETE_FORBIDDEN(HttpStatus.CONFLICT, "활성 디바이스는 삭제할 수 없습니다. 먼저 로그아웃하세요."),
    DEVICE_TRUSTED_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "안심 기기 한도를 초과했습니다. 기존 안심 기기를 먼저 해제하세요.");

    private final HttpStatus httpStatus;
    private final String message;

}