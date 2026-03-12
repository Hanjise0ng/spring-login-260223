package com.han.back.global.security.service.implement;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import com.han.back.global.security.util.RedisUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;

    @Override
    public AuthTokenDto issueTokens(Long id, Role role) {
        String accessToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_ACCESS, id, role, AuthConst.ACCESS_EXPIRATION);
        String refreshToken = jwtUtil.createJwt(AuthConst.TOKEN_TYPE_REFRESH, id, role, AuthConst.REFRESH_EXPIRATION);

        long ttl = jwtUtil.getExpiration(refreshToken);
        redisUtil.setDataExpire(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + id, refreshToken, ttl);

        return AuthTokenDto.of(accessToken, refreshToken);
    }

    @Override
    public AuthTokenDto rotateTokens(Long id, Role role, AuthTokenDto oldTokens) {
        invalidateTokens(oldTokens);
        return issueTokens(id, role);
    }

    @Override
    public void invalidateTokens(AuthTokenDto oldTokens) {
        if (oldTokens == null || oldTokens.isEmpty()) return;

        blacklistAccessToken(oldTokens);
        revokeRefreshToken(oldTokens);
    }

    @Override
    public void validateRefreshToken(Long id, String refreshToken) {
        boolean isValid = redisUtil.getData(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + id)
                .filter(refreshToken::equals)
                .isPresent();

        if (!isValid) {
            log.error("Token Mismatch or Hijacking Suspected - UserId: {}", id);
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
    }

    @Override
    public boolean isBlacklisted(String accessToken) {
        return redisUtil.hasKey(AuthConst.TOKEN_BLACKLIST_PREFIX + accessToken);
    }

    @Override
    public CustomUserDetails authenticateAccessToken(String accessToken) {
        try {
            if (isBlacklisted(accessToken)) {
                throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
            }
        } catch (CustomAuthenticationException e) { // 블랙리스트 히트
            throw e;
        } catch (CustomException e) { // Redis 장애 시 블랙리스트 확인 불가
            log.error("Redis unavailable during blacklist check - denying access | Error: {}", e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        Claims claims = jwtUtil.parseClaims(accessToken);
        if (!AuthConst.TOKEN_TYPE_ACCESS.equals(jwtUtil.getCategory(claims))) {
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        return new CustomUserDetails(jwtUtil.getId(claims), jwtUtil.getRole(claims));
    }

    @Override
    public CustomUserDetails authenticateRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.parseClaims(refreshToken);

        if (!AuthConst.TOKEN_TYPE_REFRESH.equals(jwtUtil.getCategory(claims))) {
            log.warn("Reissue Failed - Reason: Not a Refresh Token");
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        return new CustomUserDetails(jwtUtil.getId(claims), jwtUtil.getRole(claims));
    }

    private void blacklistAccessToken(AuthTokenDto oldTokens) {
        if (!oldTokens.hasAccessToken()) return;

        jwtUtil.extractClaimsLeniently(oldTokens.getAccessToken()).ifPresent(claims -> {
            long ttl = Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 0);
            if (ttl > 0) {
                redisUtil.setDataExpire(
                        AuthConst.TOKEN_BLACKLIST_PREFIX + oldTokens.getAccessToken(),
                        "logout",
                        ttl
                );
            }
        });
    }

    private void revokeRefreshToken(AuthTokenDto oldTokens) {
        if (!oldTokens.hasRefreshToken()) return;

        jwtUtil.extractClaimsLeniently(oldTokens.getRefreshToken()).ifPresent(claims -> {
            Long id = jwtUtil.getId(claims);
            redisUtil.deleteData(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + id);
        });
    }

}