package com.han.back.global.security.util;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
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

    public String createJwt(String category, Long id, Role role, long expiredMs) {
        Date now = new Date();

        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(issuer)
                .id(UUID.randomUUID().toString())
                .claim(AuthConst.TOKEN_TYPE_CATEGORY, category)
                .claim(AuthConst.TOKEN_USER_PK, id)
                .claim(AuthConst.TOKEN_ROLE, role.getAuthority())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException(BaseResponseStatus.EXPIRED_JWT_TOKEN);
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT Signature - Error: {}", e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.INVALID_JWT_SIGNATURE);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT Token - Error: {}", e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new CustomAuthenticationException(BaseResponseStatus.EMPTY_JWT_TOKEN);
        }
    }

    public Optional<Claims> extractClaimsLeniently(String token) {
        try {
            return Optional.of(
                    Jwts.parser().verifyWith(secretKey).build()
                            .parseSignedClaims(token).getPayload()
            );
        } catch (ExpiredJwtException e) {
            return Optional.ofNullable(e.getClaims());
        } catch (Exception e) {
            log.warn("JWT parsing failed leniently (ignored) - Error: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<String> extractUserPk(String token) {
        return extractClaimsLeniently(token)
                .map(claims -> claims.get(AuthConst.TOKEN_USER_PK, Number.class))
                .map(Number::longValue)
                .map(String::valueOf);
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

    public long getExpiration(String token) {
        Claims claims = parseClaims(token);
        return Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 0);
    }

}