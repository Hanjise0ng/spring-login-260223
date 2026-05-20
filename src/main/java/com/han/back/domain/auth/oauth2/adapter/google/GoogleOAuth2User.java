package com.han.back.domain.auth.oauth2.adapter.google;

import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.entity.AuthProvider;

import java.util.Map;

public class GoogleOAuth2User implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    GoogleOAuth2User(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getNickname() {
        String name = (String) attributes.get("name");
        return name != null ? name : OAuth2Const.DEFAULT_NICKNAME;
    }

}