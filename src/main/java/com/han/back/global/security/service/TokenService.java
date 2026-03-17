package com.han.back.global.security.service;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;

public interface TokenService {

    // 토큰 발급 (생성 + Redis 저장)
    AuthTokenDto issueTokens(Long id, Role role);

    // 토큰 재발급 (기존 무효화 + 신규 발급 및 저장)
    AuthTokenDto rotateTokens(Long id, Role role, AuthTokenDto oldToken);

    // 토큰 무효화 (로그아웃 등, AT 블랙리스트 + RT 삭제)
    void invalidateTokens(Long id, AuthTokenDto token);

    // Refresh Token 소유권 확인 (Redis 대조)
    void validateRefreshToken(Long id, String refreshToken);

    // Access Token 블랙리스트 확인
    boolean isBlacklisted(String accessToken);

    // Access Token 검증
    CustomUserDetails authenticateAccessToken(String accessToken);

    // Refresh Token 검증
    CustomUserDetails authenticateRefreshToken(String refreshToken);

}