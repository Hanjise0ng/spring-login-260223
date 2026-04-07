package com.han.back.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "로그인 ID 중복 확인 응답")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginIdCheckResponseDto {

    @Schema(description = "회원가입 시 필요한 로그인 ID 확인 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
    private final String loginIdToken;

    public static LoginIdCheckResponseDto of(String token) {
        return new LoginIdCheckResponseDto(token);
    }

}