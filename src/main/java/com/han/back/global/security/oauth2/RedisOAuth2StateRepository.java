package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.redis.util.RedisUtil;
import com.han.back.global.response.ResponseStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisOAuth2StateRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        return redisUtil.getData(buildKey(state))
                .map(this::deserialize)
                .orElse(null);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            String state = request.getParameter("state");
            if (state != null) {
                redisUtil.deleteData(buildKey(state));
            }
            return;
        }

        redisUtil.setDataExpire(
                buildKey(authorizationRequest.getState()),
                serialize(authorizationRequest),
                OAuth2Const.OAUTH2_STATE_TTL);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {

        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        return redisUtil.getAndDelete(buildKey(state))
                .map(this::deserialize)
                .orElse(null);
    }

    private String buildKey(String state) {
        return OAuth2Const.OAUTH2_STATE_PREFIX + state;
    }

    private String serialize(OAuth2AuthorizationRequest request) {
        try {
            return objectMapper.writeValueAsString(OAuth2StateDto.from(request));
        } catch (Exception e) {
            log.error("OAuth2 state serialization failed", e);
            throw new CustomException(ResponseStatus.SERIALIZATION_ERROR);
        }
    }

    private OAuth2AuthorizationRequest deserialize(String json) {
        try {
            return objectMapper.readValue(json, OAuth2StateDto.class)
                    .toAuthorizationRequest();
        } catch (Exception e) {
            log.error("OAuth2 state deserialization failed", e);
            throw new CustomException(ResponseStatus.SERIALIZATION_ERROR);
        }
    }

}