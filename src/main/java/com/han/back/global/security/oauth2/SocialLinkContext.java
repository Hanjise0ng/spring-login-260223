package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.infra.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SocialLinkContext {

    private final RedisUtil redisUtil;

    public void save(String state, Long userId) {
        redisUtil.setDataExpire(buildKey(state), String.valueOf(userId), OAuth2Const.SOCIAL_LINK_TOKEN_TTL);
    }

    public Optional<Long> consume(String state) {
        if (state == null) {
            return Optional.empty();
        }
        return redisUtil.getAndDelete(buildKey(state)).map(Long::valueOf);
    }

    private String buildKey(String state) {
        return OAuth2Const.SOCIAL_LINK_CONTEXT_PREFIX + state;
    }

}