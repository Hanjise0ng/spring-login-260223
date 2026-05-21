package com.han.back.global.security.token;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocialSignUpClaims {

    private final String provider;
    private final String providerId;
    private final String nickname;

    public static SocialSignUpClaims of(String provider, String providerId, String nickname) {
        return new SocialSignUpClaims(provider, providerId, nickname);
    }

}