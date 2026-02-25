package com.han.back.global.config;

import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Component
public class CorsConfig extends UrlBasedCorsConfigurationSource {

    public CorsConfig() {
        CorsConfiguration config = new CorsConfiguration();

        // 모든 프론트엔드 출처 허용
        config.setAllowedOriginPatterns(List.of("*"));

        // 허용할 HTTP 메서드 명시 (OPTIONS 포함 필수)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 모든 커스텀 헤더 허용
        config.setAllowedHeaders(List.of("*"));

        // JWT 토큰 및 쿠키 인증 허용
        config.setAllowCredentials(true);

        // Preflight 요청 캐싱 (1시간) - 네트워크 성능 최적화
        config.setMaxAge(3600L);

        // 위 설정들을 API 경로(/api/v1/**)에 적용
        this.registerCorsConfiguration("/api/v1/**", config);
    }
}