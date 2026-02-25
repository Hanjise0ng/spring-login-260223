package com.han.back.global.exception;

import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.dto.Empty;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Empty>> handleCustomException(CustomException e, HttpServletRequest request) {
        log.warn("CustomException occurred at {}: [{}] {}",
                request.getRequestURI(),
                e.getStatus().getCode(),
                e.getStatus().getMessage());
        return BaseResponse.error(e.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Empty>> handleValidationExceptions(MethodArgumentNotValidException e, HttpServletRequest request) {
        String detailMessage = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed at {}: {}", request.getRequestURI(), detailMessage);
        return BaseResponse.error(BaseResponseStatus.VALIDATION_FAIL, detailMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Empty>> handleJsonParsingExceptions(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("JSON parsing failed at {}: {}", request.getRequestURI(), e.getMessage());
        return BaseResponse.error(BaseResponseStatus.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<BaseResponse<Empty>> handleDataAccessException(DataAccessException e, HttpServletRequest request) {
        log.warn("DataAccessException occurred at {}: {}", request.getRequestURI(), e.getMessage());
        return BaseResponse.error(BaseResponseStatus.DATABASE_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Empty>> handleException(Exception e, HttpServletRequest request) {
        log.error("Unhandled Exception occurred at {}: ", request.getRequestURI(), e);
        return BaseResponse.error(BaseResponseStatus.INTERNAL_SERVER_ERROR);
    }

}