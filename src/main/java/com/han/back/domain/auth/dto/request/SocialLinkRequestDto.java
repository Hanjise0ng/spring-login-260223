package com.han.back.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "소셜 연동 본인확인 요청 (기존 LOCAL 계정 자격증명)")
@Getter
@AllArgsConstructor
public class SocialLinkRequestDto {

    @Schema(description = "연동 대상 LOCAL 계정의 로그인 ID", example = "testuser01")
    @NotBlank(message = "아이디가 필요합니다.")
    private final String loginId;

    @Schema(description = "연동 대상 LOCAL 계정의 비밀번호", example = "Test1234!")
    @NotBlank(message = "비밀번호가 필요합니다.")
    private final String password;

}