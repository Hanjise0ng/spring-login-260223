package com.han.back.controller.docs;

import com.fasterxml.jackson.annotation.JsonView;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.exception.DeviceResponseStatus;
import com.han.back.global.docs.ApiErrorCode;
import com.han.back.global.docs.ApiErrorCodes;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.response.ResponseView;
import com.han.back.global.security.principal.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@Tag(name = "Device", description = "디바이스 관리 API")
@SecurityRequirement(name = "bearerAuth")
public interface DeviceApiDocs {

    @Operation(summary = "내 디바이스 목록 조회",
            description = "현재 로그인한 사용자의 전체 디바이스 목록을 반환합니다. "
                    + "현재 디바이스와 활성 세션 여부가 표시됩니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<List<DeviceDetailResponseDto>>> getMyDevices(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "안심 기기 등록",
            description = "특정 디바이스를 안심 기기로 등록합니다. "
                    + "안심 기기는 최대 세션 초과 시 자동 퇴출 대상에서 제외됩니다.")
    @ApiResponse(responseCode = "200", description = "등록 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL"),
            @ApiErrorCode(value = DeviceResponseStatus.class, constant = "DEVICE_NOT_FOUND"),
            @ApiErrorCode(value = DeviceResponseStatus.class, constant = "DEVICE_TRUSTED_LIMIT_EXCEEDED")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<Empty>> trustDevice(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            String devicePublicId);

    @Operation(summary = "안심 기기 해제", description = "안심 기기 등록을 해제합니다.")
    @ApiResponse(responseCode = "200", description = "해제 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL"),
            @ApiErrorCode(value = DeviceResponseStatus.class, constant = "DEVICE_NOT_FOUND")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<Empty>> untrustDevice(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            String devicePublicId);

    @Operation(summary = "디바이스 강제 로그아웃",
            description = "특정 디바이스의 세션을 강제로 종료합니다. "
                    + "현재 사용 중인 디바이스는 강제 로그아웃할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "강제 로그아웃 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = DeviceResponseStatus.class, constant = "DEVICE_SELF_LOGOUT_FORBIDDEN"),
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL"),
            @ApiErrorCode(value = DeviceResponseStatus.class, constant = "DEVICE_NOT_FOUND")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<Empty>> forceLogoutDevice(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "디바이스 공개 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            String devicePublicId);

    @Operation(summary = "디바이스 삭제",
            description = "비활성 상태인 디바이스를 삭제합니다. "
                    + "활성 세션이 있는 디바이스는 먼저 강제 로그아웃 후 삭제해야 합니다.")
    @ApiResponse(responseCode = "200", description = "삭제 성공")
    @ApiErrorCodes({
            @ApiErrorCode(value = AuthResponseStatus.class, constant = "AUTH_AUTHENTICATION_FAIL"),
            @ApiErrorCode(value = DeviceResponseStatus.class, constant = "DEVICE_NOT_FOUND"),
            @ApiErrorCode(value = DeviceResponseStatus.class, constant = "DEVICE_ACTIVE_DELETE_FORBIDDEN")
    })
    @JsonView(ResponseView.Common.class)
    ResponseEntity<BaseResponse<Empty>> deleteDevice(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "디바이스 공개 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            String devicePublicId);

}