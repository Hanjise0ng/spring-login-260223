package com.han.back.domain.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuth2SignInResult {

    private final String code;

    public static OAuth2SignInResult of(String code) {
        return new OAuth2SignInResult(code);
    }

}