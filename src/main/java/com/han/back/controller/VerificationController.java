package com.han.back.controller;

import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Verification", description = "인증 코드 발송/확인 API")
public class VerificationController {

    private final VerificationService verificationService;

    @Operation(summary = "인증 코드 발송",
            description = "이메일 또는 SMS로 인증 코드를 발송합니다. "
                    + "쿨다운 기간 내 재요청 시 429가 반환됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "발송 성공 — 만료 시간 포함"),
            @ApiResponse(responseCode = "400", description = "VF: 유효성 검증 실패"),
            @ApiResponse(responseCode = "422", description = "UNC: 지원하지 않는 알림 채널"),
            @ApiResponse(responseCode = "429", description = "CA: 쿨다운 중 (잠시 후 재요청)"),
            @ApiResponse(responseCode = "500", description = """
                    - MF: 메일 발송 실패
                    - SSF: SMS 발송 실패""")
    })
    @PostMapping("/send")
    public ResponseEntity<BaseResponse<VerificationSendResponseDto>> sendCode(
            @Valid @RequestBody VerificationSendRequestDto request) {

        VerificationSendResponseDto response = verificationService.sendCode(request);
        return BaseResponse.success(response);
    }

    @Operation(summary = "인증 코드 확인",
            description = "발송된 인증 코드를 검증합니다. "
                    + "인증 성공 시 해당 대상 + 인증 용도 조합이 '인증 완료' 상태로 저장됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = """
                    - VF: 유효성 검증 실패
                    - CF: 인증 코드 불일치
                    - VE: 인증 코드 만료""")
    })
    @PostMapping("/confirm")
    public ResponseEntity<BaseResponse<Empty>> confirmCode(
            @Valid @RequestBody VerificationConfirmRequestDto request) {

        verificationService.confirmCode(request);
        return BaseResponse.success();
    }

}