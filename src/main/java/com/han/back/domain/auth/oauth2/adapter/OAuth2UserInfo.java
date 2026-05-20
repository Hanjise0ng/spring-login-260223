package com.han.back.domain.auth.oauth2.adapter;

import com.han.back.domain.user.entity.AuthProvider;

public interface OAuth2UserInfo {

    AuthProvider getProvider();

    String getProviderId();

    String getEmail();

    String getNickname();

}