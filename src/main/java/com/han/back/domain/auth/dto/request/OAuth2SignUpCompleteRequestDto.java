package com.han.back.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class OAuth2SignUpCompleteRequestDto {

    @NotBlank(message = "Social sign-up token is required.")
    private final String tempToken;

    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    private final String email;

    public OAuth2SignUpCompleteRequestDto(String tempToken, String email) {
        this.tempToken = tempToken;
        this.email = email;
    }

}