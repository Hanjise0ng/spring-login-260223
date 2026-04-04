package com.han.back.global.security.util;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Component
public class LoginIdTokenUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String SEPARATOR = ":";

    private final String secretKey;

    public LoginIdTokenUtil(@Value("${auth.login-id-token.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    public String issue(String loginId) {
        long expiresAt = System.currentTimeMillis() + AuthConst.LOGIN_ID_TOKEN_TTL;

        String payload = loginId + SEPARATOR + expiresAt;
        String signature = sign(payload);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                (payload + SEPARATOR + signature).getBytes(StandardCharsets.UTF_8)
        );
    }

    public void validate(String loginId, String token) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);

            int lastSep = decoded.lastIndexOf(SEPARATOR);
            String payload = decoded.substring(0, lastSep);
            String receivedSignature = decoded.substring(lastSep + 1);

            if (!MessageDigest.isEqual(
                    sign(payload).getBytes(StandardCharsets.UTF_8),
                    receivedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("LoginId token signature mismatch - loginId: {}", loginId);
                throw new CustomException(BaseResponseStatus.LOGIN_ID_CHECK_REQUIRED);
            }

            String[] parts = payload.split(SEPARATOR, 2);
            String tokenLoginId = parts[0];
            long expiresAt = Long.parseLong(parts[1]);

            if (!tokenLoginId.equals(loginId)) {
                log.warn("LoginId token loginId mismatch - expected: {}, actual: {}", loginId, tokenLoginId);
                throw new CustomException(BaseResponseStatus.LOGIN_ID_CHECK_REQUIRED);
            }

            if (System.currentTimeMillis() > expiresAt) {
                throw new CustomException(BaseResponseStatus.LOGIN_ID_CHECK_REQUIRED);
            }

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.warn("LoginId token validation failed - loginId: {}, cause: {}", loginId, e.getMessage());
            throw new CustomException(BaseResponseStatus.LOGIN_ID_CHECK_REQUIRED);
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM
            ));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            log.error("HMAC signing failed", e);
            throw new CustomException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

}