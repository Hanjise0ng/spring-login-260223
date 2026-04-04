package com.han.back.controller;

import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.Empty;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/verification")
public class VerificationController {

    private final VerificationService verificationService;

    @PostMapping("/send")
    public ResponseEntity<BaseResponse<VerificationSendResponseDto>> sendCode(
            @Valid @RequestBody VerificationSendRequestDto request) {

        VerificationSendResponseDto response = verificationService.sendCode(request);
        return BaseResponse.success(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<BaseResponse<Empty>> confirmCode(
            @Valid @RequestBody VerificationConfirmRequestDto request) {

        verificationService.confirmCode(request);
        return BaseResponse.success();
    }

}
