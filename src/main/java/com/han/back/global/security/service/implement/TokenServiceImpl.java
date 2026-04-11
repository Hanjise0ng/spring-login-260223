package com.han.back.global.security.service.implement;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import com.han.back.global.infra.redis.util.RedisUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;

    @Override
    public AuthToken issueTokens(Long id, Role role, String sessionId) {
        return createAndStoreTokens(id, role, sessionId);
    }

    @Override
    public AuthToken rotateTokens(Long id, Role role, String oldSessionId, String newSessionId) {
        invalidateSession(id, oldSessionId);
        return createAndStoreTokens(id, role, newSessionId);
    }

    @Override
    public void invalidateSession(Long id, String sessionId) {
        blacklistSession(sessionId);
        revokeRefreshToken(id, sessionId);
    }

    @Override
    public CustomUserDetails authenticateAccessToken(String accessToken) {
        Claims claims = jwtUtil.parseClaims(accessToken);

        if (!AuthConst.TOKEN_TYPE_ACCESS.equals(jwtUtil.getCategory(claims))) {
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        String sessionId = jwtUtil.getSessionId(claims);

        try {
            if (isSessionBlacklisted(sessionId)) {
                throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
            }
        } catch (CustomAuthenticationException e) { // 블랙리스트 히트 — 정상 인증 거부 흐름
            throw e;
        } catch (CustomException e) { // Redis 장애 — 블랙리스트 확인 불가, 보안 우선으로 인증 거부
            log.error("Redis unavailable during session blacklist check - denying access | SessionId: {} | Error: {}",
                    sessionId, e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        return new CustomUserDetails(jwtUtil.getId(claims), jwtUtil.getRole(claims), sessionId);
    }

    @Override
    public CustomUserDetails authenticateRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.parseClaims(refreshToken);

        if (!AuthConst.TOKEN_TYPE_REFRESH.equals(jwtUtil.getCategory(claims))) {
            log.warn("Reissue Failed - Reason: Not a Refresh Token");
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        return new CustomUserDetails(jwtUtil.getId(claims), jwtUtil.getRole(claims), jwtUtil.getSessionId(claims));
    }

    @Override
    public void validateRefreshToken(Long id, String sessionId, String refreshToken) {
        String redisKey = buildRefreshKey(id, sessionId);

        boolean isValid = redisUtil.getData(redisKey)
                .filter(refreshToken::equals)
                .isPresent();

        if (!isValid) {
            log.error("Token Mismatch or Hijacking Suspected - UserPK: {} | SessionId: {}", id, sessionId);
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
    }

    @Override
    public boolean isSessionBlacklisted(String sessionId) {
        return redisUtil.hasKey(AuthConst.TOKEN_SESSION_BLACKLIST_PREFIX + sessionId);
    }

    @Override
    public Optional<CustomUserDetails> extractUserFromTokens(String accessToken, String refreshToken) {
        Optional<Claims> rtClaims = jwtUtil.extractClaimsLeniently(refreshToken);
        if (rtClaims.isEmpty()) return Optional.empty();

        Claims rt = rtClaims.get();
        if (!AuthConst.TOKEN_TYPE_REFRESH.equals(jwtUtil.getCategory(rt))) {
            return Optional.empty();
        }

        Long id = jwtUtil.getId(rt);
        Role role = jwtUtil.getRole(rt);

        String sessionId = jwtUtil.extractClaimsLeniently(accessToken)
                .map(jwtUtil::getSessionId)
                .orElseGet(() -> jwtUtil.getSessionId(rt));

        return Optional.of(new CustomUserDetails(id, role, sessionId));
    }

    private AuthToken createAndStoreTokens(Long id, Role role, String sessionId) {
        String accessToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_ACCESS, id, role, sessionId, AuthConst.ACCESS_EXPIRATION);
        String refreshToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_REFRESH, id, role, sessionId, AuthConst.REFRESH_EXPIRATION);

        redisUtil.setDataExpire(buildRefreshKey(id, sessionId), refreshToken, AuthConst.REFRESH_EXPIRATION);
        return AuthToken.of(accessToken, refreshToken);
    }

    private void blacklistSession(String sessionId) {
        redisUtil.setDataExpire(AuthConst.TOKEN_SESSION_BLACKLIST_PREFIX + sessionId, "revoked", AuthConst.ACCESS_EXPIRATION);
    }

    private void revokeRefreshToken(Long id, String sessionId) {
        redisUtil.deleteData(buildRefreshKey(id, sessionId));
    }

    private String buildRefreshKey(Long id, String sessionId) {
        return AuthConst.TOKEN_REFRESH_REDIS_PREFIX + id + ":" + sessionId;
    }

}