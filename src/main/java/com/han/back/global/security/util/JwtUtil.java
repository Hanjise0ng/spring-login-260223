package com.han.back.global.security.util;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
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

    public Claims validateAndGetPayload(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT Signature - Error: {}", e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.INVALID_JWT_SIGNATURE);
        } catch (ExpiredJwtException e) {
            throw new CustomAuthenticationException(BaseResponseStatus.EXPIRED_JWT_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT Token - Error: {}", e.getMessage());
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        } catch (IllegalArgumentException e) {
            throw new CustomAuthenticationException(BaseResponseStatus.EMPTY_JWT_TOKEN);
        }
    }

    public Long getUserId(Claims claims) {
        Number userId = claims.get(AuthConst.TOKEN_USER_ID, Number.class);
        if (userId == null) {
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
        return userId.longValue();
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

    public Long getUserId(String token) {
        return getUserId(validateAndGetPayload(token));
    }

    public String getCategory(String token) {
        return getCategory(validateAndGetPayload(token));
    }

    public Role getRole(String token) {
        return getRole(validateAndGetPayload(token));
    }

    public long getExpiration(String token) {
        Claims claims = validateAndGetPayload(token);
        return Math.max(claims.getExpiration().getTime() - System.currentTimeMillis(), 0);
    }

    public boolean isExpired(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (SignatureException | SecurityException | MalformedJwtException | UnsupportedJwtException |
                 IllegalArgumentException e) {
            log.warn("Invalid JWT Token (isExpired) - Error: {}", e.getMessage());
            return true;
        }
    }

    public String createJwt(String category, Long userId, Role role, long expiredMs) {
        Date now = new Date();

        return Jwts.builder()
                .header().type("JWT").and()
                .issuer(issuer)
                .id(UUID.randomUUID().toString())
                .claim(AuthConst.TOKEN_TYPE_CATEGORY, category)
                .claim(AuthConst.TOKEN_USER_ID, userId)
                .claim(AuthConst.TOKEN_ROLE, role.getAuthority())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

}