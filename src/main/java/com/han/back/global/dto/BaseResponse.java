package com.han.back.global.dto;

import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Getter
public class BaseResponse<T> {

    private final String code;
    private final String message;
    private final T result;

    private BaseResponse(BaseResponseStatus status, T result) {
        this.code = status.getCode();
        this.message = status.getMessage();
        this.result = result;
    }

    private BaseResponse(BaseResponseStatus status, String customMessage) {
        this.code = status.getCode();
        this.message = customMessage;
        this.result = null;
    }

    // Spring MVC (Controller)
    public static ResponseEntity<BaseResponse<Empty>> success() {
        return ResponseEntity
                .status(BaseResponseStatus.SUCCESS.getHttpStatus())
                .body(successBody());
    }

    public static <T> ResponseEntity<BaseResponse<T>> success(T result) {
        return ResponseEntity
                .status(BaseResponseStatus.SUCCESS.getHttpStatus())
                .body(successBody(result));
    }

    public static ResponseEntity<BaseResponse<Empty>> error(BaseResponseStatus status) {
        return ResponseEntity
                .status(status.getHttpStatus())
                .body(errorBody(status));
    }

    public static ResponseEntity<BaseResponse<Empty>> error(BaseResponseStatus status, String customMessage) {
        return ResponseEntity
                .status(status.getHttpStatus())
                .body(errorBody(status, customMessage));
    }

    // Servlet Filter / Handler (ResponseEntity 생성 없이 body만)
    public static BaseResponse<Empty> successBody() {
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, Empty.getInstance());
    }

    public static <T> BaseResponse<T> successBody(T result) {
        return new BaseResponse<>(BaseResponseStatus.SUCCESS, result);
    }

    public static BaseResponse<Empty> errorBody(BaseResponseStatus status) {
        return new BaseResponse<>(status, Empty.getInstance());
    }

    public static BaseResponse<Empty> errorBody(BaseResponseStatus status, String customMessage) {
        return new BaseResponse<>(status, customMessage);
    }

}