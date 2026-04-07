package com.han.back.global.exception;

import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.dto.Empty;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Empty>> handleCustomException(CustomException e, HttpServletRequest request) {
        // 서비스·도메인 레이어에서 명시적으로 발생시킨 비즈니스 오류
        log.warn("CustomException occurred at {}: [{}] {}",
                request.getRequestURI(), e.getStatus().getCode(), e.getStatus().getMessage());
        return BaseResponse.error(e.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Empty>> handleValidationExceptions(MethodArgumentNotValidException e, HttpServletRequest request) {
        // @Valid 검증 실패 — @RequestBody 필드 제약조건 위반
        String detailMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed at {}: {}", request.getRequestURI(), detailMessage);
        return BaseResponse.error(BaseResponseStatus.VALIDATION_FAIL, detailMessage);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<BaseResponse<Empty>> handleHandlerMethodValidationException(
            HandlerMethodValidationException e, HttpServletRequest request) {
        // @RequestParam, @PathVariable 등 메서드 파라미터 레벨 검증 실패
        String detailMessage = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Handler method validation failed at {}: {}", request.getRequestURI(), detailMessage);
        return BaseResponse.error(BaseResponseStatus.VALIDATION_FAIL, detailMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Empty>> handleJsonParsingExceptions(HttpMessageNotReadableException e, HttpServletRequest request) {
        // 요청 바디 역직렬화 실패 — 잘못된 JSON 형식 또는 타입 불일치
        log.warn("JSON parsing failed at {}: {}", request.getRequestURI(), e.getMessage());
        return BaseResponse.error(BaseResponseStatus.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<BaseResponse<Empty>> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        // JPA·JDBC 레이어 데이터베이스 접근 오류
        log.warn("DataAccessException occurred at {}: {}", request.getRequestURI(), e.getMessage());
        return BaseResponse.error(BaseResponseStatus.DATABASE_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Empty>> handleException(Exception e, HttpServletRequest request) {
        // 위 핸들러에서 처리되지 않은 모든 예외 — 예상치 못한 런타임 오류
        log.error("Unhandled Exception occurred at {}: ", request.getRequestURI(), e);
        return BaseResponse.error(BaseResponseStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CustomAuthenticationException.class)
    public ResponseEntity<BaseResponse<Empty>> handleCustomAuthenticationException(
            CustomAuthenticationException e, HttpServletRequest request) {
        log.warn("AuthenticationException occurred at {}: [{}] {}",
                request.getRequestURI(), e.getStatus().getCode(), e.getStatus().getMessage());
        return BaseResponse.error(e.getStatus());
    }

}