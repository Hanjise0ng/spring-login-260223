package com.han.back.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignInRequestDto {

    @NotBlank(message = "아이디가 필요합니다.")
    private final String loginId;

    @NotBlank(message = "비밀번호가 필요합니다.")
    private final String password;

}