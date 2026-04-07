package com.han.back.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Schema(description = "회원가입 요청")
@Getter
@Setter
@NoArgsConstructor
public class SignUpRequestDto {

    @Schema(description = "로그인 ID", example = "testuser01")
    @NotBlank(message = "Login ID is required.")
    private String loginId;

    @Schema(description = "비밀번호 (영문 + 숫자 포함, 8~13자)", example = "Test1234!")
    @NotBlank(message = "Password is required.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9!@#$%^&*]{8,13}$")
    private String password;

    @Schema(description = "이메일 (인증 완료 필수)", example = "user@example.com")
    @NotBlank(message = "Email is required.")
    @Email(message = "Invalid email format.")
    private String email;

    @Schema(description = "닉네임 (2~20자)", example = "테스트유저")
    @NotBlank(message = "Nickname is required.")
    @Size(min = 2, max = 20)
    private String nickname;

    @Schema(description = "로그인 ID 중복 확인 시 발급받은 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank(message = "Login ID check token is required.")
    private String loginIdToken;

}