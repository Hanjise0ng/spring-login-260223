package com.han.back.domain.verification.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VerificationSendResponseDto {

    private final long codeExpiresIn;
    private final long cooldownExpiresIn;

    public static VerificationSendResponseDto of(long codeExpiresInMillis, long cooldownExpiresInMillis) {
        return new VerificationSendResponseDto(
                codeExpiresInMillis / 1_000,
                cooldownExpiresInMillis / 1_000
        );
    }

}