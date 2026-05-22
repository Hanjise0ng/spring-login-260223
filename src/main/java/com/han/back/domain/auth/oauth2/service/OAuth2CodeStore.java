package com.han.back.domain.auth.oauth2.service;

import com.han.back.domain.auth.dto.OAuth2CodePayload;
import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.redis.util.RedisUtil;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2CodeStore {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public String save(SignInResult signInResult) {
        String code = UuidUtil.generateString();
        OAuth2CodePayload payload = OAuth2CodePayload.from(signInResult);

        try {
            String json = objectMapper.writeValueAsString(payload);
            redisUtil.setDataExpire(buildKey(code), json, OAuth2Const.OAUTH2_CODE_TTL);
        } catch (Exception e) {
            log.error("OAuth2 code payload serialization failed", e);
            throw new IllegalStateException("Failed to serialize OAuth2 code payload", e);
        }

        return code;
    }

    public OAuth2CodePayload consume(String code) {
        String key = buildKey(code);
        String json = redisUtil.getData(key)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.AUTHENTICATION_FAIL));

        redisUtil.deleteData(key);

        try {
            return objectMapper.readValue(json, OAuth2CodePayload.class);
        } catch (Exception e) {
            log.error("OAuth2 code payload deserialization failed", e);
            throw new CustomException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
    }

    private String buildKey(String code) {
        return OAuth2Const.OAUTH2_CODE_PREFIX + code;
    }

}