package com.han.back.global.docs;

import com.han.back.global.response.ApiResponseStatus;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;

public final class ApiErrorCodeParser {

    private ApiErrorCodeParser() {}

    public static void parse(Operation operation, HandlerMethod handlerMethod) {
        List<ApiErrorCode> errorCodes = collect(handlerMethod);
        if (errorCodes.isEmpty()) {
            return;
        }

        ApiResponses responses = operation.getResponses();
        for (ApiErrorCode errorCode : errorCodes) {
            ApiResponseStatus status = resolveStatus(errorCode);
            ApiErrorCodeWriter.write(responses, status, errorCode.summary());
        }
    }

    private static List<ApiErrorCode> collect(HandlerMethod handlerMethod) {
        ApiErrorCodes container = handlerMethod.getMethodAnnotation(ApiErrorCodes.class);
        if (container != null) {
            return Arrays.asList(container.value());
        }
        ApiErrorCode single = handlerMethod.getMethodAnnotation(ApiErrorCode.class);
        if (single != null) {
            return List.of(single);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E> & ApiResponseStatus> ApiResponseStatus resolveStatus(ApiErrorCode annotation) {
        Class<E> enumType = (Class<E>) annotation.value();
        try {
            return Enum.valueOf(enumType, annotation.constant());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "존재하지 않는 응답 상수 참조: " + enumType.getSimpleName() + "." + annotation.constant(), e);
        }
    }

}