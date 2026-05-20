package com.han.back.domain.auth.oauth2.adapter.naver;

import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserAdapter;
import com.han.back.domain.user.entity.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NaverOAuth2UserAdapter implements OAuth2UserAdapter {

    @Override
    public AuthProvider getSupportedProvider() {
        return AuthProvider.NAVER;
    }

    @Override
    public OAuth2UserInfo convert(Map<String, Object> attributes) {
        return new NaverOAuth2User(attributes);
    }

}