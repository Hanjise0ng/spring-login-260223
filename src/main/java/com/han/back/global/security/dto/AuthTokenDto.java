package com.han.back.global.security.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthTokenDto {

    private String accessToken;

    private String refreshToken;

    public static AuthTokenDto of(String accessToken, String refreshToken) {
        return AuthTokenDto.builder()
                .accessToken(StringUtils.hasText(accessToken) ? accessToken : "")
                .refreshToken(StringUtils.hasText(refreshToken) ? refreshToken : "")
                .build();
    }

}