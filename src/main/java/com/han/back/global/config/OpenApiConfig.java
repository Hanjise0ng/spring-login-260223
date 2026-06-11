package com.han.back.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
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
                        | code | String | 응답 코드 (SUCCESS, VALIDATION_FAIL, AUTH_SIGN_IN_FAIL 등) |
                        | message | String | 응답 메시지 |
                        | result | Object | 응답 데이터 (없으면 빈 객체) |
                        
                        ---
                        
                        ## 에러 코드 — 400 Bad Request
                        | Code | 설명 |
                        |------|------|
                        | VALIDATION_FAIL | 요청 값 검증 실패 |
                        | MALFORMED_REQUEST_BODY | 요청 본문 형식 오류 |
                        | AUTH_LOGIN_ID_CHECK_REQUIRED | 로그인 ID 중복 확인 필요 |
                        | ACCOUNT_PASSWORD_SAME_AS_CURRENT | 새 비밀번호가 현재와 동일 |
                        | ACCOUNT_PASSWORD_CONFIRM_MISMATCH | 비밀번호 확인 불일치 |
                        | ACCOUNT_SOCIAL_ONLY | 소셜 전용 계정은 불가 |
                        | ACCOUNT_TAG_GENERATION_FAIL | 태그 생성 실패 |
                        | VERIFY_CODE_MISMATCH | 인증 코드 불일치 |
                        | VERIFY_CODE_EXPIRED | 인증 코드 만료 |
                        | VERIFY_NOT_COMPLETED | 인증 미완료 |
                        | DEVICE_SELF_LOGOUT_FORBIDDEN | 현재 디바이스 강제 로그아웃 불가 |

                        ## 에러 코드 — 401 Unauthorized
                        | Code | 설명 |
                        |------|------|
                        | AUTH_SIGN_IN_FAIL | 로그인 정보 불일치 |
                        | AUTH_AUTHENTICATION_FAIL | 인증 실패 |
                        | AUTH_INVALID_PASSWORD | 비밀번호 오류 |
                        | AUTH_JWT_SIGNATURE_INVALID | JWT 서명 오류 |
                        | AUTH_JWT_UNSUPPORTED | 지원하지 않는 JWT |
                        | AUTH_JWT_EMPTY | JWT 비어 있음 |
                        | AUTH_ACCESS_TOKEN_MISSING | Access Token 누락 |
                        | AUTH_ACCESS_TOKEN_EXPIRED | Access Token 만료 |
                        | AUTH_REFRESH_TOKEN_MISSING | Refresh Token 누락 |
                        | AUTH_REFRESH_TOKEN_EXPIRED | Refresh Token 만료 (재로그인 필요) |
                        | AUTH_REFRESH_TOKEN_INVALID | Refresh Token 무효 |
                        | ACCOUNT_PASSWORD_RESET_TOKEN_INVALID | 비밀번호 재설정 토큰 무효/만료 |

                        ## 에러 코드 — 403 Forbidden
                        | Code | 설명 |
                        |------|------|
                        | FORBIDDEN | 접근 권한 없음 |
                        | DEVICE_BANNED | 디바이스 차단됨 |
                        | ACCOUNT_STEP_UP_REQUIRED | 재인증 필요 |

                        ## 에러 코드 — 404 Not Found
                        | Code | 설명 |
                        |------|------|
                        | ACCOUNT_USER_NOT_FOUND | 사용자 없음 |
                        | RESOURCE_NOT_FOUND | 리소스 없음 |
                        | DEVICE_NOT_FOUND | 디바이스 없음 |

                        ## 에러 코드 — 409 Conflict
                        | Code | 설명 |
                        |------|------|
                        | ACCOUNT_DUPLICATE_LOGIN_ID | 아이디 중복 |
                        | ACCOUNT_DUPLICATE_EMAIL | 이메일 중복 |
                        | ACCOUNT_ALREADY_DELETED | 이미 탈퇴된 계정 |
                        | ACCOUNT_NICKNAME_TAG_DUPLICATE | 닉네임+태그 조합 중복 |
                        | SOCIAL_ALREADY_LINKED | 소셜 계정 이미 연동 |
                        | DEVICE_ACTIVE_DELETE_FORBIDDEN | 활성 디바이스 삭제 불가 |
                        | DEVICE_TRUSTED_LIMIT_EXCEEDED | 안심 기기 한도 초과 |

                        ## 에러 코드 — 422 / 429 / 500
                        | Code | HTTP | 설명 |
                        |------|------|------|
                        | SOCIAL_PROVIDER_UNSUPPORTED | 422 | 지원하지 않는 소셜 제공자 |
                        | VERIFY_CHANNEL_UNSUPPORTED | 422 | 지원하지 않는 알림 채널 |
                        | VERIFY_COOLDOWN | 429 | 요청 쿨다운 |
                        | RATE_LIMIT_EXCEEDED | 429 | 요청 한도 초과 |
                        | VERIFY_MAIL_TEMPLATE_FAIL | 500 | 메일 템플릿 처리 실패 |
                        | VERIFY_MAIL_SEND_FAIL | 500 | 메일 발송 실패 |
                        | VERIFY_SMS_SEND_FAIL | 500 | SMS 발송 실패 |
                        | DB_ERROR | 500 | 데이터베이스 오류 |
                        | REDIS_ERROR | 500 | Redis 오류 |
                        | SERIALIZATION_ERROR | 500 | 직렬화 오류 |
                        | INTERNAL_SERVER_ERROR | 500 | 내부 서버 오류 |
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