package com.han.back.domain.auth.oauth2.adapter;

import com.han.back.domain.user.entity.AuthProvider;

import java.util.Map;

public interface OAuth2UserAdapter {

    AuthProvider getSupportedProvider();

    OAuth2UserInfo convert(Map<String, Object> attributes);

}