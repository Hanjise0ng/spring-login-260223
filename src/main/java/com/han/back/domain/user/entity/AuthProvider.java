package com.han.back.domain.user.entity;

import com.han.back.global.response.BaseResponseStatus;
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
                .filter(provider -> provider.getValue().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new CustomException(BaseResponseStatus.UNSUPPORTED_SOCIAL_PROVIDER));
    }

    public boolean isSocial() {
        return this != LOCAL;
    }

}