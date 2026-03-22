package com.han.back.global.security.service;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;

import java.util.Optional;

public interface TokenService {

    /** 신규 세션 생성 + 토큰 쌍 발급 (로그인 시) */
    AuthTokenDto issueTokens(Long id, Role role, String sessionId);

    /** 기존 세션 유지 + 토큰 쌍 재발급 (reissue 시) */
    AuthTokenDto rotateTokens(Long id, Role role, String sessionId, AuthTokenDto oldTokens);

    /** 세션 단위 무효화 — 세션 블랙리스트 등록 + RT 삭제 */
    void invalidateSession(Long id, String sessionId);

    /** AT 검증 — 블랙리스트 확인 + claims 파싱 */
    CustomUserDetails authenticateAccessToken(String accessToken);

    /** RT 검증 — claims 파싱 + 카테고리 확인 */
    CustomUserDetails authenticateRefreshToken(String refreshToken);

    /** RT 소유권 확인 — Redis 저장값과 대조 */
    void validateRefreshToken(Long id, String sessionId, String refreshToken);

    /** 세션 블랙리스트 확인 */
    boolean isSessionBlacklisted(String sessionId);

    /** AT/RT에서 사용자 정보 추출 — 로그아웃 시 AT 만료 fallback용 */
    Optional<CustomUserDetails> extractUserFromTokens(String accessToken, String refreshToken);

}