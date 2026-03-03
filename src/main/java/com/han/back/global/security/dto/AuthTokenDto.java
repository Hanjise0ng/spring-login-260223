package com.han.back.global.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthTokenDto {

    private String accessToken;

    private String refreshToken;

}