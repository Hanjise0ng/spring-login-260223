package com.han.back.domain.verification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "인증 코드 발송 응답")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VerificationSendResponseDto {

    @Schema(description = "인증 코드 만료까지 남은 시간 (초)", example = "300")
    private final long codeExpiresIn;

    @Schema(description = "재발송 쿨다운 남은 시간 (초)", example = "60")
    private final long cooldownExpiresIn;

    public static VerificationSendResponseDto of(long codeExpiresInMillis, long cooldownExpiresInMillis) {
        return new VerificationSendResponseDto(
                codeExpiresInMillis / 1_000,
                cooldownExpiresInMillis / 1_000
        );
    }

}