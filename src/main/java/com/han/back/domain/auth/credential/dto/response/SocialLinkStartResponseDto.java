package com.han.back.domain.auth.credential.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "소셜 연동 시작 응답")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocialLinkStartResponseDto {

    @Schema(description = "OAuth 시작 시 link_token 파라미터로 전달할 서명 토큰")
    private final String linkToken;

    public static SocialLinkStartResponseDto of(String linkToken) {
        return new SocialLinkStartResponseDto(linkToken);
    }

}