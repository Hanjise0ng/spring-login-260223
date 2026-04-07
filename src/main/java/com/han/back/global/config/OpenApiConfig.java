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
                        | code | String | 응답 코드 (SU, VF, SF 등) |
                        | message | String | 응답 메시지 |
                        | result | Object | 응답 데이터 (없으면 빈 객체) |
                        
                        ---
                        
                        ## 에러 코드 — 400 Bad Request
                        | Code | 설명 |
                        |------|------|
                        | VF | Validation 실패 |
                        | CF | 인증 코드 불일치 |
                        | IRB | 요청 본문 형식 오류 |
                        | LICR | 로그인 ID 중복 확인 필요 |
                        | SCP | 현재 비밀번호와 동일 |
                        | PCM | 비밀번호 확인 불일치 |
                        | VE | 인증 코드 만료 |
                        | VNC | 인증 미완료 |
                        | SOA | 소셜 전용 계정 |
                        | SDL | 현재 디바이스 강제 로그아웃 불가 |
                        
                        ## 에러 코드 — 401 Unauthorized
                        | Code | 설명 |
                        |------|------|
                        | SF | 로그인 정보 불일치 |
                        | AUF | 인증 실패 |
                        | IPW | 비밀번호 오류 |
                        | PRI | 비밀번호 재설정 토큰 만료 |
                        | EJT | Access Token 만료 |
                        | IJS | JWT 서명 오류 |
                        | MAT | Access Token 누락 |
                        | MRT | Refresh Token 누락 |
                        | ERT | Refresh Token 만료 (재로그인 필요) |
                        | IRT | Refresh Token 무효 |
                        
                        ## 에러 코드 — 403 Forbidden
                        | Code | 설명 |
                        |------|------|
                        | NP | 권한 없음 |
                        | DB | 디바이스 차단됨 |
                        | SUR | 재인증 필요 |
                        
                        ## 에러 코드 — 404 Not Found
                        | Code | 설명 |
                        |------|------|
                        | NFU | 사용자 없음 |
                        | NFR | 리소스 없음 |
                        | NFD | 디바이스 없음 |
                        
                        ## 에러 코드 — 409 Conflict
                        | Code | 설명 |
                        |------|------|
                        | DI | 아이디 중복 |
                        | DE | 이메일 중복 |
                        | AD | 이미 삭제됨 |
                        | SAL | 소셜 계정 이미 연동 |
                        | ACD | 활성 디바이스 삭제 불가 |
                        
                        ## 에러 코드 — 422 / 429 / 500
                        | Code | HTTP | 설명 |
                        |------|------|------|
                        | USP | 422 | 지원하지 않는 소셜 제공자 |
                        | UNC | 422 | 지원하지 않는 알림 채널 |
                        | CA | 429 | 요청 쿨다운 |
                        | MF | 500 | 메일 발송 실패 |
                        | SSF | 500 | SMS 발송 실패 |
                        | DBE | 500 | 데이터베이스 오류 |
                        | RE | 500 | Redis 오류 |
                        | ISE | 500 | 내부 서버 오류 |
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