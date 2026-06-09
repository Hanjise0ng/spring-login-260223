package com.han.back.global.exception;

import com.han.back.global.response.ApiResponseStatus;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.response.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Spring MVC 내부 예외 폴백 — 개별 오버라이드에서 처리되지 않은 나머지
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {

        String uri = extractUri(request);
        ApiResponseStatus mappedStatus = mapHttpStatus(statusCode);

        if (statusCode.is5xxServerError()) {
            log.error("Spring MVC internal server error - uri: {} | status: {} | message: {}",
                    uri, statusCode.value(), ex.getMessage(), ex);
        } else {
            log.warn("Spring MVC client error - uri: {} | status: {} | message: {}",
                    uri, statusCode.value(), ex.getMessage());
        }

        return ResponseEntity
                .status(statusCode)
                .headers(headers)
                .body(BaseResponse.errorBody(mappedStatus));
    }

    // @RequestBody @Valid 검증 실패 — 필드 오류 메시지 추출 및 BaseResponse 포맷 적용
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String detailMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation failed - uri: {} | detail: {}", extractUri(request), detailMessage);

        return ResponseEntity
                .status(status)
                .body(BaseResponse.errorBody(ResponseStatus.VALIDATION_FAIL, detailMessage));
    }

    // @RequestBody 역직렬화 실패 — 잘못된 JSON 형식 또는 타입 불일치
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("JSON parsing failed - uri: {} | message: {}", extractUri(request), ex.getMessage());

        return ResponseEntity
                .status(status)
                .body(BaseResponse.errorBody(ResponseStatus.MALFORMED_REQUEST_BODY));
    }

    // @RequestParam, @PathVariable 검증 실패 — 메서드 파라미터 레벨 @Valid 위반
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String detailMessage = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream())
                .map(MessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Handler method validation failed - uri: {} | detail: {}",
                extractUri(request), detailMessage);

        return ResponseEntity
                .status(status)
                .body(BaseResponse.errorBody(ResponseStatus.VALIDATION_FAIL, detailMessage));
    }

    // HTTP 메서드 불일치 — 존재하는 엔드포인트에 허용되지 않은 메서드로 요청
    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Method not allowed - uri: {} | method: {}",
                extractUri(request), ex.getMethod());

        return ResponseEntity
                .status(status)
                .body(BaseResponse.errorBody(ResponseStatus.METHOD_NOT_ALLOWED));
    }

    // 지원하지 않는 미디어 타입 — Content-Type 불일치
    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.warn("Unsupported media type - uri: {} | contentType: {}",
                extractUri(request), ex.getContentType());

        return ResponseEntity
                .status(status)
                .body(BaseResponse.errorBody(ResponseStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    // 비즈니스 예외 — 서비스·도메인 레이어에서 명시적으로 던진 오류
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Empty>> handleCustomException(
            CustomException ex, HttpServletRequest request) {

        log.warn("CustomException - uri: {} | code: [{}] | message: {}",
                request.getRequestURI(), ex.getStatus().getCode(), ex.getStatus().getMessage());

        return BaseResponse.error(ex.getStatus());
    }

    // Controller 레이어 인증 예외 — 필터 체인 외부에서 발생한 인증 오류
    @ExceptionHandler(CustomAuthenticationException.class)
    public ResponseEntity<BaseResponse<Empty>> handleCustomAuthenticationException(
            CustomAuthenticationException ex, HttpServletRequest request) {

        log.warn("AuthenticationException - uri: {} | code: [{}] | message: {}",
                request.getRequestURI(), ex.getStatus().getCode(), ex.getStatus().getMessage());

        return BaseResponse.error(ex.getStatus());
    }

    // DB 접근 예외 — JPA·JDBC 오류, 내부 세부사항 추상화
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<BaseResponse<Empty>> handleDataAccessException(
            DataAccessException ex, HttpServletRequest request) {

        log.error("DataAccessException - uri: {} | message: {}",
                request.getRequestURI(), ex.getMessage(), ex);

        return BaseResponse.error(ResponseStatus.DB_ERROR);
    }

    // 위 핸들러에서 처리되지 않은 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Empty>> handleException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception - uri: {}", request.getRequestURI(), ex);

        return BaseResponse.error(ResponseStatus.INTERNAL_SERVER_ERROR);
    }

    private String extractUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return request.getDescription(false);
    }

    private ApiResponseStatus mapHttpStatus(HttpStatusCode statusCode) {
        if (statusCode.is4xxClientError()) {
            return ResponseStatus.VALIDATION_FAIL;
        }
        return ResponseStatus.INTERNAL_SERVER_ERROR;
    }

}