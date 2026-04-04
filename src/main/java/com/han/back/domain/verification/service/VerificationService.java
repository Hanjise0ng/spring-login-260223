package com.han.back.domain.verification.service;

import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.entity.VerificationType;

public interface VerificationService {

    VerificationSendResponseDto sendCode(VerificationSendRequestDto request);

    void confirmCode(VerificationConfirmRequestDto request);

    void validateConfirmed(String target, VerificationType type);

    void consumeConfirmation(String target, VerificationType type);

}