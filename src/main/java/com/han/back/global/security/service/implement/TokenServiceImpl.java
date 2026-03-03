package com.han.back.global.security.service.implement;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import com.han.back.global.security.util.RedisUtil;
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
        if (!StringUtils.hasText(oldTokens.getAccessToken()) && !StringUtils.hasText(oldTokens.getRefreshToken())) {
            return;
        }

        // Access Token 무효화
        if (StringUtils.hasText(oldTokens.getAccessToken()) && !jwtUtil.isExpired(oldTokens.getAccessToken())) {
            long ttl = jwtUtil.getExpiration(oldTokens.getAccessToken());
            redisUtil.setDataExpire(AuthConst.TOKEN_BLACKLIST_PREFIX + oldTokens.getAccessToken(), "logout", ttl);
            log.info("Access Token Blacklisted - TTL: {}ms", ttl);
        }

        // Refresh Token 무효화
        if (StringUtils.hasText(oldTokens.getRefreshToken()) && !jwtUtil.isExpired(oldTokens.getRefreshToken())) {
            Long userId = jwtUtil.getUserId(oldTokens.getRefreshToken());
            redisUtil.deleteData(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userId);
            log.info("Refresh Token Deleted - UserId: {}", userId);
        }
    }

    @Override
    public void verifyRefreshTokenOwnership(Long id, String refreshToken) {
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

}