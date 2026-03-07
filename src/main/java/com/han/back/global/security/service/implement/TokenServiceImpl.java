package com.han.back.global.security.service.implement;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
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
import org.springframework.util.StringUtils;

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

        return AuthTokenDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthTokenDto rotateTokens(Long id, Role role, AuthTokenDto oldTokens) {
        invalidateTokens(oldTokens);
        return issueTokens(id, role);
    }

    @Override
    public void invalidateTokens(AuthTokenDto oldTokens) {
        if (oldTokens == null) return;
        if (!StringUtils.hasText(oldTokens.getAccessToken()) && !StringUtils.hasText(oldTokens.getRefreshToken())) {
            return;
        }

        if (StringUtils.hasText(oldTokens.getAccessToken())) {
            Claims claims = jwtUtil.parseClaimsIgnoreExpiry(oldTokens.getAccessToken());
            long ttl = Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 0);
            if (ttl > 0) {
                redisUtil.setDataExpire(AuthConst.TOKEN_BLACKLIST_PREFIX + oldTokens.getAccessToken(), "logout", ttl);
                log.info("Access Token Blacklisted - TTL: {}ms", ttl);
            } else {
                log.info("Access Token already expired, skip blacklist - ExpiredAt: {}", claims.getExpiration());
            }
        }

        if (StringUtils.hasText(oldTokens.getRefreshToken())) {
            Claims claims = jwtUtil.parseClaimsIgnoreExpiry(oldTokens.getRefreshToken());
            Long id = jwtUtil.getUserId(claims);
            redisUtil.deleteData(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + id);
            log.info("Refresh Token Deleted - UserId: {}", id);
        }
    }

    @Override
    public void validateRefreshToken(Long id, String refreshToken) {
        String storedToken = redisUtil.getData(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + id);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
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
        if (isBlacklisted(accessToken)) {
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        Claims claims = jwtUtil.parseClaims(accessToken);
        if (!AuthConst.TOKEN_TYPE_ACCESS.equals(jwtUtil.getCategory(claims))) {
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        return new CustomUserDetails(jwtUtil.getUserId(claims), jwtUtil.getRole(claims));
    }

    @Override
    public CustomUserDetails authenticateRefreshToken(String refreshToken) {
        Claims claims = jwtUtil.parseClaims(refreshToken);

        if (!AuthConst.TOKEN_TYPE_REFRESH.equals(jwtUtil.getCategory(claims))) {
            log.warn("Reissue Failed - Reason: Not a Refresh Token");
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        return new CustomUserDetails(jwtUtil.getUserId(claims), jwtUtil.getRole(claims));
    }

}