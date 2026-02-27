package com.han.back.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SignInResponseDto {

    private final String accessToken;
    private final Long expirationTime;

    public static SignInResponseDto of(String accessToken, Long expirationTime) {
        return SignInResponseDto.builder()
                .accessToken(accessToken)
                .expirationTime(expirationTime)
                .build();
    }

}