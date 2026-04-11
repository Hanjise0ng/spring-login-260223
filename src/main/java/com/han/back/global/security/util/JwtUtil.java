package com.han.back.global.security.util;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
public class JwtUtil {

    @Value("${spring.jwt.issuer}")
    private String issuer;

    private final SecretKey secretKey;

    public JwtUtil(@Value("${spring.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String createJwt(String category, Long id, Role role, String sessionId, long expiredMs) {
        Date now = new Date();

        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(issuer)
                .id(UUID.randomUUID().toString())
                .claim(AuthConst.TOKEN_TYPE_CATEGORY, category)
                .claim(AuthConst.TOKEN_USER_PK, id)
                .claim(AuthConst.TOKEN_ROLE, role.getAuthority())
                .claim(AuthConst.TOKEN_SESSION_ID, sessionId)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) { // 토큰 만료
            throw new CustomAuthenticationException(BaseResponseStatus.EXPIRED_JWT_TOKEN);
        } catch (SecurityException | MalformedJwtException e) { // 서명 불일치 또는 토큰 구조 손상
            log.warn("Invalid JWT Signature - Error: {}", e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.INVALID_JWT_SIGNATURE);
        } catch (UnsupportedJwtException e) { // 지원하지 않는 JWT 알고리즘 또는 형식
            log.warn("Unsupported JWT Token - Error: {}", e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) { // 토큰 문자열이 null 또는 빈 값
            throw new CustomAuthenticationException(BaseResponseStatus.EMPTY_JWT_TOKEN);
        }
    }

    public Optional<Claims> extractClaimsLeniently(String token) {
        if (!StringUtils.hasText(token)) return Optional.empty();

        try {
            return Optional.of(
                    Jwts.parser().verifyWith(secretKey).build()
                            .parseSignedClaims(token).getPayload()
            );
        } catch (ExpiredJwtException e) { // 만료된 토큰 (claims는 추출 가능)
            return Optional.ofNullable(e.getClaims());
        } catch (Exception e) { // 위조 또는 손상되어 복구 불가
            log.warn("JWT parsing failed leniently (ignored) - Error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Long getId(Claims claims) {
        Number id = claims.get(AuthConst.TOKEN_USER_PK, Number.class);
        if (id == null) {
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
        return id.longValue();
    }

    public Role getRole(Claims claims) {
        String roleStr = claims.get(AuthConst.TOKEN_ROLE, String.class);

        if (!StringUtils.hasText(roleStr)) {
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        Role role = Role.fromAuthority(roleStr);

        if (role == null) {
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        return role;
    }

    public String getCategory(Claims claims) {
        return claims.get(AuthConst.TOKEN_TYPE_CATEGORY, String.class);
    }

    public String getSessionId(Claims claims) {
        String sessionId = claims.get(AuthConst.TOKEN_SESSION_ID, String.class);
        if (!StringUtils.hasText(sessionId)) {
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
        return sessionId;
    }

    public long getRemainingExpiration(Claims claims) {
        return Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 0);
    }

}