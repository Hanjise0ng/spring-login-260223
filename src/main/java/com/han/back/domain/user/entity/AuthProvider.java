package com.han.back.domain.user.entity;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum AuthProvider {

    LOCAL("local"),
    KAKAO("kakao"),
    NAVER("naver"),
    GOOGLE("google");

    private final String value;

    public static AuthProvider fromRegistrationId(String registrationId) {
        return Arrays.stream(values())
                .filter(provider -> provider.getValue().equalsIgnoreCase(registrationId))
                .findFirst()
                .orElseThrow(() -> new CustomException(BaseResponseStatus.UNSUPPORTED_SOCIAL_PROVIDER));
    }

}