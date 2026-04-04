package com.han.back.domain.auth.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginIdCheckResponseDto {

    private final String loginIdToken;

    public static LoginIdCheckResponseDto of(String loginIdToken) {
        return new LoginIdCheckResponseDto(loginIdToken);
    }

}