package com.han.back.global.security.token.util;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.global.exception.CustomException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialLinkTokenUtil {

    private final JwtUtil jwtUtil;

    public String issue(Long userId) {
        Map<String, Object> claims = Map.of(OAuth2Const.CLAIM_LINK_USER_ID, userId);
        return jwtUtil.createTempToken(
                OAuth2Const.TOKEN_CATEGORY_SOCIAL_LINK,
                OAuth2Const.SOCIAL_LINK_TOKEN_TTL.toMillis(),
                claims);
    }

    public Long validate(String token) {
        try {
            Claims claims = jwtUtil.parseClaims(token);

            String category = jwtUtil.getCategory(claims);
            if (!OAuth2Const.TOKEN_CATEGORY_SOCIAL_LINK.equals(category)) {
                throw new CustomException(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID);
            }

            return claims.get(OAuth2Const.CLAIM_LINK_USER_ID, Long.class);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Social link token validation failed: {}", e.getMessage());
            throw new CustomException(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID);
        }
    }

}