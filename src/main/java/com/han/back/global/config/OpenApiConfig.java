package com.han.back.global.config;

import com.han.back.global.docs.ApiErrorCodeParser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Slf4j
@Configuration
@Profile("!prod")
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, bearerScheme()));
    }

    @Bean
    public OperationCustomizer errorCodeOperationCustomizer() {
        return (operation, handlerMethod) -> {
            ApiErrorCodeParser.parse(operation, handlerMethod);
            return operation;
        };
    }

    private Info apiInfo() {
        return new Info()
                .title("HAN Project API")
                .version("v1.0.0")
                .description("""
                        ## 인증 방식
                        - Access Token → Authorization: Bearer {AT} 헤더로 전송
                        - Refresh Token → HttpOnly 쿠키 (refresh_token)
                        - Device ID → HttpOnly 쿠키 (device_id)

                        ## 공통 응답 구조

                        | 필드 | 타입 | 설명 |
                        |------|------|------|
                        | code | String | 응답 코드 (SUCCESS / 도메인별 에러 코드) |
                        | message | String | 응답 메시지 |
                        | traceId | String | 요청 추적 식별자 (오류 응답에만 포함) |
                        | result | Object | 응답 데이터 (없으면 빈 객체) |

                        > 각 엔드포인트가 반환할 수 있는 에러 코드는 해당 API의 응답 예시에서 확인
                        """);
    }

    private SecurityScheme bearerScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Access Token을 입력하세요. (Bearer 접두사 불필요)");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printSwaggerUrl(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");

        log.info("\n" +
                        "─────────────────────────────────────────────────────────────────────────\n" +
                        "\t📚 Swagger UI : http://localhost:{}{}/swagger-ui/index.html\n" +
                        "─────────────────────────────────────────────────────────────────────────",
                port, contextPath);
    }

}