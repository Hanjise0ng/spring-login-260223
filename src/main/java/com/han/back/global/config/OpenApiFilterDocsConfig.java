package com.han.back.global.config;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.global.docs.ApiErrorCodeWriter;
import com.han.back.global.response.ApiResponseStatus;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.util.SecurityPathConst;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

@Configuration
@Profile("!prod")
public class OpenApiFilterDocsConfig {

    @Bean
    public OpenApiCustomizer filterEndpointDocs() {
        return openApi -> {
            openApi.path(SecurityPathConst.LOGIN_PATH, signInPath());
            openApi.path(SecurityPathConst.LOGOUT_PATH, logoutPath());
        };
    }

    private PathItem signInPath() {
        ObjectSchema requestSchema = new ObjectSchema();
        requestSchema.addProperty("loginId",
                new StringSchema().example("user1234").description("로그인 ID"));
        requestSchema.addProperty("password",
                new StringSchema().example("user1234!").description("비밀번호"));
        requestSchema.setRequired(List.of("loginId", "password"));

        ApiResponses responses = new ApiResponses()
                .addApiResponse("200", successResponse("로그인 성공 — AT(헤더), RT(쿠키), device_id(쿠키)"));
        addErrors(responses, ResponseStatus.MALFORMED_REQUEST_BODY, AuthResponseStatus.AUTH_SIGN_IN_FAIL);

        Operation operation = new Operation()
                .tags(List.of("Auth"))
                .summary("로그인")
                .description("""
                        LoginFilter가 처리하는 JSON 기반 로그인.

                        **성공 시 응답:**
                        - Authorization 헤더: Access Token
                        - Set-Cookie: refresh_token (HttpOnly)
                        - Set-Cookie: device_id (HttpOnly)

                        **실패 시:** 401 AUTH_SIGN_IN_FAIL
                        """)
                .requestBody(new RequestBody()
                        .required(true)
                        .content(jsonContent(requestSchema)))
                .responses(responses);

        return new PathItem().post(operation);
    }

    private PathItem logoutPath() {
        ApiResponses responses = new ApiResponses()
                .addApiResponse("200", successResponse("로그아웃 성공"));
        addErrors(responses, AuthResponseStatus.AUTH_AUTHENTICATION_FAIL, ResponseStatus.REDIS_ERROR);

        Operation operation = new Operation()
                .tags(List.of("Auth"))
                .summary("로그아웃")
                .description("""
                        CustomLogoutHandler가 처리하는 로그아웃.

                        Authorization 헤더(AT) + refresh_token 쿠키(RT)를 함께 전송해야 합니다.
                        AT가 만료된 경우에도 RT를 통한 fallback 처리로 로그아웃이 가능합니다.

                        **처리 내용:**
                        - 세션 블랙리스트 등록
                        - Refresh Token 삭제
                        - 디바이스 세션 비활성화
                        """)
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .responses(responses);

        return new PathItem().post(operation);
    }

    private void addErrors(ApiResponses responses, ApiResponseStatus... statuses) {
        for (ApiResponseStatus status : statuses) {
            ApiErrorCodeWriter.write(responses, status, null);
        }
    }

    private Content jsonContent(Schema<?> schema) {
        return new Content().addMediaType("application/json",
                new MediaType().schema(schema));
    }

    private ApiResponse successResponse(String description) {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("code", new StringSchema().example("SUCCESS"));
        schema.addProperty("message", new StringSchema().example("성공"));
        schema.addProperty("result", new ObjectSchema().description("빈 객체 ({})"));

        return new ApiResponse()
                .description(description)
                .content(jsonContent(schema));
    }

}