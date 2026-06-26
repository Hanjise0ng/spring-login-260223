package com.han.back.global.docs;

import com.han.back.global.response.ApiResponseStatus;
import com.han.back.global.response.BaseResponse;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiErrorCodeWriter {

    private static final String JSON_MEDIA_TYPE = "application/json";

    private ApiErrorCodeWriter() {}

    public static void write(ApiResponses responses, ApiResponseStatus status, String summary) {
        String httpStatus = String.valueOf(status.getHttpStatus().value());

        ApiResponse response = responses.computeIfAbsent(httpStatus, key -> new ApiResponse());
        if (!StringUtils.hasText(response.getDescription())) {
            response.setDescription(status.getHttpStatus().getReasonPhrase());
        }

        Content content = response.getContent();
        if (content == null) {
            content = new Content();
            response.setContent(content);
        }

        MediaType mediaType = content.get(JSON_MEDIA_TYPE);
        if (mediaType == null) {
            mediaType = new MediaType().schema(errorSchemaRef());
            content.addMediaType(JSON_MEDIA_TYPE, mediaType);
        }

        String exampleName = StringUtils.hasText(summary) ? summary : status.getMessage();
        mediaType.addExamples(exampleName, buildExample(status, exampleName));
    }

    private static Example buildExample(ApiResponseStatus status, String summary) {
        Example example = new Example();
        example.setSummary(summary);
        example.setValue(errorBody(status.getCode(), status.getMessage()));
        return example;
    }

    private static Schema<?> errorSchemaRef() {
        Schema<?> schema = new Schema<>();
        schema.set$ref("#/components/schemas/" + BaseResponse.class.getSimpleName());
        return schema;
    }

    private static Map<String, Object> errorBody(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("result", Map.of());
        return body;
    }

}