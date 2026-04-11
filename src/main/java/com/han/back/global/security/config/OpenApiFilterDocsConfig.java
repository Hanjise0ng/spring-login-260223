package com.han.back.global.security.config;

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
            openApi.path("/api/v1/auth/sign-in", signInPath());
            openApi.path("/api/v1/auth/logout", logoutPath());
        };
    }

    private PathItem signInPath() {
        ObjectSchema requestSchema = new ObjectSchema();
        requestSchema.addProperty("loginId",
                new StringSchema().example("testuser01").description("로그인 ID"));
        requestSchema.addProperty("password",
                new StringSchema().example("Test1234!").description("비밀번호"));
        requestSchema.setRequired(List.of("loginId", "password"));

        Operation operation = new Operation()
                .tags(List.of("Auth"))
                .summary("로그인")
                .description("""
                        LoginFilter가 처리하는 JSON 기반 로그인.
                        
                        **성공 시 응답:**
                        - Authorization 헤더: Access Token
                        - Set-Cookie: refresh_token (HttpOnly)
                        - Set-Cookie: device_id (HttpOnly)
                        
                        **실패 시:** 401 SF
                        """)
                .requestBody(new RequestBody()
                        .required(true)
                        .content(jsonContent(requestSchema)))
                .responses(new ApiResponses()
                        .addApiResponse("200", successResponse("로그인 성공 — AT(헤더), RT(쿠키), device_id(쿠키)"))
                        .addApiResponse("400", errorResponse("IRB", "요청 본문 형식 오류"))
                        .addApiResponse("401", errorResponse("SF", "로그인 정보 불일치")));

        return new PathItem().post(operation);
    }

    private PathItem logoutPath() {
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
                .responses(new ApiResponses()
                        .addApiResponse("200", successResponse("로그아웃 성공"))
                        .addApiResponse("401", errorResponse("AUF", "인증 실패")));

        return new PathItem().post(operation);
    }

    private Content jsonContent(Schema<?> schema) {
        return new Content().addMediaType("application/json",
                new MediaType().schema(schema));
    }

    private ApiResponse successResponse(String description) {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("code", new StringSchema().example("SU"));
        schema.addProperty("message", new StringSchema().example("Success."));
        schema.addProperty("result", new ObjectSchema().description("빈 객체 ({})"));

        return new ApiResponse()
                .description(description)
                .content(jsonContent(schema));
    }

    private ApiResponse errorResponse(String code, String message) {
        ObjectSchema schema = new ObjectSchema();
        schema.addProperty("code", new StringSchema().example(code));
        schema.addProperty("message", new StringSchema().example(message));
        schema.addProperty("result", new ObjectSchema().description("빈 객체 ({})"));

        return new ApiResponse()
                .description(message)
                .content(jsonContent(schema));
    }

}