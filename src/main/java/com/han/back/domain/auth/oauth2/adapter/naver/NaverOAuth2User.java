package com.han.back.domain.auth.oauth2.adapter.naver;

import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.entity.AuthProvider;

import java.util.Map;

public class NaverOAuth2User implements OAuth2UserInfo {

    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    NaverOAuth2User(Map<String, Object> attributes) {
        Object responseObj = attributes.get("response");
        this.response = responseObj instanceof Map ? (Map<String, Object>) responseObj : Map.of();
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.NAVER;
    }

    @Override
    public String getProviderId() {
        return (String) response.get("id");
    }

    @Override
    public String getEmail() {
        return (String) response.get("email");
    }

    @Override
    public String getNickname() {
        String name = (String) response.get("name");
        return name != null ? name : OAuth2Const.DEFAULT_NICKNAME;
    }

}