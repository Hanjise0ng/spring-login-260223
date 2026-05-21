package com.han.back.global.security.token;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialSignUpTokenUtil {

    private final JwtUtil jwtUtil;

    public String issue(String provider, String providerId, String nickname) {
        Map<String, Object> claims = Map.of(
                AuthConst.TEMP_PROVIDER, provider,
                AuthConst.TEMP_PROVIDER_ID, providerId,
                "nickname", nickname
        );
        return jwtUtil.createTempToken(
                OAuth2Const.TOKEN_CATEGORY_SOCIAL_SIGN_UP,
                OAuth2Const.SOCIAL_SIGN_UP_TOKEN_TTL.toMillis(),
                claims);
    }

    public SocialSignUpClaims validate(String token) {
        try {
            Claims claims = jwtUtil.parseClaims(token);

            String category = jwtUtil.getCategory(claims);
            if (!OAuth2Const.TOKEN_CATEGORY_SOCIAL_SIGN_UP.equals(category)) {
                throw new CustomException(BaseResponseStatus.SOCIAL_SIGN_UP_TOKEN_INVALID);
            }

            return SocialSignUpClaims.of(
                    claims.get(AuthConst.TEMP_PROVIDER, String.class),
                    claims.get(AuthConst.TEMP_PROVIDER_ID, String.class),
                    claims.get(AuthConst.CLAIM_NICKNAME, String.class));

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Social sign-up token validation failed: {}", e.getMessage());
            throw new CustomException(BaseResponseStatus.SOCIAL_SIGN_UP_TOKEN_INVALID);
        }
    }

}