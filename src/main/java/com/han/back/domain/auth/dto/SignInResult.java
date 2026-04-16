package com.han.back.domain.auth.dto;

import com.han.back.global.security.token.AuthToken;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignInResult {

    private final AuthToken tokens;
    private final String deviceFingerprint;

    public static SignInResult of(AuthToken tokens, String deviceFingerprint) {
        return new SignInResult(tokens, deviceFingerprint);
    }

}