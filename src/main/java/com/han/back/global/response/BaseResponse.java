package com.han.back.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Schema(description = "공통 응답 래퍼")
@Getter
public class BaseResponse<T> {

    @Schema(description = "응답 코드", example = "SU")
    private final String code;

    @Schema(description = "응답 메시지", example = "Success.")
    private final String message;

    @Schema(description = "응답 데이터 (없으면 빈 객체)")
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