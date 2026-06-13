package com.han.back.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "탈퇴 유예 계정 복구 안내 응답")
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RecoveryGuidanceResponseDto {

    @Schema(description = "남은 유예 기간(일)", example = "23")
    private final long gracePeriodRemainingDays;

    public static RecoveryGuidanceResponseDto of(long gracePeriodRemainingDays) {
        return new RecoveryGuidanceResponseDto(gracePeriodRemainingDays);
    }

}