package com.han.back.domain.auth.credential.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "소셜 단독 계정의 로컬 계정 생성(승격) 요청")
@Getter
@AllArgsConstructor
public class LocalCredentialCreateRequestDto {

    @Schema(description = "로그인 ID", example = "testuser01")
    @NotBlank(message = "아이디가 필요합니다.")
    private final String loginId;

    @Schema(description = "비밀번호 (영문 + 숫자 포함, 8~13자)", example = "Test1234!")
    @NotBlank(message = "비밀번호가 필요합니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9!@#$%^&*]{8,13}$")
    private final String password;

    @Schema(description = "로그인 ID 중복 확인 시 발급받은 토큰")
    @NotBlank(message = "로그인 ID 확인 토큰이 필요합니다.")
    private final String loginIdToken;

}