package com.han.back.domain.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuth2CodePayload {

    private String accessToken;
    private String refreshToken;
    private String deviceFingerprint;

    public static OAuth2CodePayload from(SignInResult result) {
        return new OAuth2CodePayload(
                result.getTokens().getAccessToken(),
                result.getTokens().getRefreshToken(),
                result.getDeviceFingerprint());
    }

}