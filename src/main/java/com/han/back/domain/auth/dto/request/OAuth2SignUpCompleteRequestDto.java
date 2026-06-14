package com.han.back.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuth2SignUpCompleteRequestDto {

    @NotBlank(message = "이메일이 필요합니다.")
    @Email(message = "잘못된 이메일 형식입니다.")
    private final String email;

    public OAuth2SignUpCompleteRequestDto(String email) {
        this.email = email;
    }

}