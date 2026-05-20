package com.han.back.domain.auth.oauth2.adapter.kakao;

import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.entity.AuthProvider;

import java.util.Map;

public class KakaoOAuth2User implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    KakaoOAuth2User(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }

    @Override
    public String getProviderId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = castMap(attributes, "kakao_account");
        if (kakaoAccount == null) {
            return null;
        }

        Boolean isVerified = (Boolean) kakaoAccount.get("is_email_verified");
        if (!Boolean.TRUE.equals(isVerified)) {
            return null;
        }

        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getNickname() {
        Map<String, Object> kakaoAccount = castMap(attributes, "kakao_account");
        if (kakaoAccount == null) {
            return OAuth2Const.DEFAULT_NICKNAME;
        }

        Map<String, Object> profile = castMap(kakaoAccount, "profile");
        if (profile == null) {
            return OAuth2Const.DEFAULT_NICKNAME;
        }

        String nickname = (String) profile.get("nickname");
        return nickname != null ? nickname : OAuth2Const.DEFAULT_NICKNAME;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

}