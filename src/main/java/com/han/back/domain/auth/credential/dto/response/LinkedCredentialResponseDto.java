package com.han.back.domain.auth.credential.dto.response;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.user.entity.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "연동된 소셜 계정 정보")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LinkedCredentialResponseDto {

    @Schema(description = "소셜 제공자", example = "KAKAO")
    private final AuthProvider provider;

    public static LinkedCredentialResponseDto of(CredentialEntity credential) {
        return new LinkedCredentialResponseDto(credential.getProvider());
    }

}