package com.han.back.domain.verification.dto.request;

import com.han.back.domain.verification.entity.VerificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerificationConfirmRequestDto {

    @NotBlank(message = "Target is required.")
    private String target;

    @NotBlank(message = "Verification code is required.")
    private String code;

    @NotNull(message = "Verification type is required.")
    private VerificationType type;

}