package com.han.back.domain.user.entity;

import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {

    LOCAL("LOCAL"),
    KAKAO("KAKAO"),
    NAVER("NAVER"),
    GOOGLE("GOOGLE");

    private final String value;

    public static AuthProvider fromRegistrationId(String registrationId) {
        return Arrays.stream(values())
                .filter(provider -> provider.getValue().equalsIgnoreCase(normalizeRegistrationId(registrationId)))
                .findFirst()
                .orElseThrow(() -> new CustomException(SocialResponseStatus.SOCIAL_PROVIDER_UNSUPPORTED));
    }

    private static String normalizeRegistrationId(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new CustomException(SocialResponseStatus.SOCIAL_PROVIDER_UNSUPPORTED);
        }

        return registrationId.split("-", 2)[0];
    }

    public boolean isSocial() {
        return this != LOCAL;
    }

}