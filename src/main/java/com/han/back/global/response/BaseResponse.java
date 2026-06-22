package com.han.back.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonView;
import com.han.back.global.trace.TraceContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Schema(description = "공통 응답 래퍼")
@Getter
public class BaseResponse<T> {

    @Schema(description = "응답 코드", example = "SUCCESS")
    @JsonView(ResponseView.Common.class)
    private final String code;

    @Schema(description = "응답 메시지(개발자용)", example = "성공")
    @JsonView(ResponseView.Common.class)
    private final String message;

    @Schema(description = "요청 추적 식별자 (오류 응답에만 포함)", example = "3f9a1c2e-7b8d-4e2a-9c6f-1a2b3c4d5e6f")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonView(ResponseView.Error.class)
    private final String traceId;

    @Schema(description = "응답 데이터 (없으면 빈 객체)")
    @JsonView(ResponseView.Common.class)
    private final T result;

    private BaseResponse(ApiResponseStatus status, String message, String traceId, T result) {
        this.code = status.getCode();
        this.message = message;
        this.traceId = traceId;
        this.result = result;
    }

    // Spring MVC (Controller)
    public static ResponseEntity<BaseResponse<Empty>> success() {
        return ResponseEntity
                .status(ResponseStatus.SUCCESS.getHttpStatus())
                .body(successBody());
    }

    public static <T> ResponseEntity<BaseResponse<T>> success(T result) {
        return ResponseEntity
                .status(ResponseStatus.SUCCESS.getHttpStatus())
                .body(successBody(result));
    }

    public static ResponseEntity<BaseResponse<Empty>> error(ApiResponseStatus status) {
        return ResponseEntity
                .status(status.getHttpStatus())
                .body(errorBody(status));
    }

    public static ResponseEntity<BaseResponse<Empty>> error(ApiResponseStatus status, String customMessage) {
        return ResponseEntity
                .status(status.getHttpStatus())
                .body(errorBody(status, customMessage));
    }

    // Servlet Filter / Handler (ResponseEntity 생성 없이 body만)
    public static BaseResponse<Empty> successBody() {
        return new BaseResponse<>(ResponseStatus.SUCCESS, ResponseStatus.SUCCESS.getMessage(), null, Empty.getInstance());
    }

    public static <T> BaseResponse<T> successBody(T result) {
        return new BaseResponse<>(ResponseStatus.SUCCESS, ResponseStatus.SUCCESS.getMessage(), null, result);
    }

    public static BaseResponse<Empty> errorBody(ApiResponseStatus status) {
        return new BaseResponse<>(status, status.getMessage(), TraceContext.getTraceIdOrNull(), Empty.getInstance());
    }

    public static BaseResponse<Empty> errorBody(ApiResponseStatus status, String customMessage) {
        return new BaseResponse<>(status, customMessage, TraceContext.getTraceIdOrNull(), Empty.getInstance());
    }

    public static <T> BaseResponse<T> body(ApiResponseStatus status, T result) {
        return new BaseResponse<>(status, status.getMessage(), TraceContext.getTraceIdOrNull(), result);
    }

}