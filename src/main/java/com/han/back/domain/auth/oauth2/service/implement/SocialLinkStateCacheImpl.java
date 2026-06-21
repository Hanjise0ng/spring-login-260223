package com.han.back.domain.auth.oauth2.service.implement;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.service.SocialLinkStateCache;
import com.han.back.global.infra.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SocialLinkStateCacheImpl implements SocialLinkStateCache {

    private final RedisUtil redisUtil;

    @Override
    public void save(String state, Long userId) {
        redisUtil.setDataExpire(buildKey(state), String.valueOf(userId), OAuth2Const.SOCIAL_LINK_TOKEN_TTL);
    }

    @Override
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