package com.han.back.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuth2SignUpCompleteRequestDto {

    @NotBlank(message = "소셜 가입 토큰이 필요합니다.")
    private final String tempToken;

    @NotBlank(message = "이메일이 필요합니다.")
    @Email(message = "잘못된 이메일 형식입니다.")
    private final String email;

    public OAuth2SignUpCompleteRequestDto(String tempToken, String email) {
        this.tempToken = tempToken;
        this.email = email;
    }

}