package com.han.back.controller;

import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.Empty;
import com.han.back.global.security.dto.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@Tag(name = "Device", description = "디바이스 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "내 디바이스 목록 조회",
            description = "현재 로그인한 사용자의 전체 디바이스 목록을 반환합니다. "
                    + "현재 디바이스와 활성 세션 여부가 표시됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "AUF: 인증 실패")
    })
    @GetMapping
    public ResponseEntity<BaseResponse<List<DeviceDetailResponseDto>>> getMyDevices(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<DeviceDetailResponseDto> devices = deviceService.getDevices(
                userDetails.getId(), userDetails.getSessionId()
        );
        return BaseResponse.success(devices);
    }

    @Operation(summary = "디바이스 강제 로그아웃",
            description = "특정 디바이스의 세션을 강제로 종료합니다. "
                    + "현재 사용 중인 디바이스는 강제 로그아웃할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "강제 로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "SDL: 현재 디바이스는 강제 로그아웃 불가 (일반 로그아웃 사용)"),
            @ApiResponse(responseCode = "401", description = "AUF: 인증 실패"),
            @ApiResponse(responseCode = "404", description = "NFD: 디바이스를 찾을 수 없음")
    })
    @PostMapping("/{devicePublicId}/logout")
    public ResponseEntity<BaseResponse<Empty>> forceLogoutDevice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "디바이스 공개 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String devicePublicId) {

        deviceService.forceLogoutDevice(
                userDetails.getId(), devicePublicId, userDetails.getSessionId()
        );
        return BaseResponse.success();
    }

    @Operation(summary = "디바이스 삭제",
            description = "비활성 상태인 디바이스를 삭제합니다. "
                    + "활성 세션이 있는 디바이스는 먼저 강제 로그아웃 후 삭제해야 합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "AUF: 인증 실패"),
            @ApiResponse(responseCode = "404", description = "NFD: 디바이스를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "ACD: 활성 디바이스는 삭제 불가 (강제 로그아웃 먼저 필요)")
    })
    @DeleteMapping("/{devicePublicId}")
    public ResponseEntity<BaseResponse<Empty>> deleteDevice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "디바이스 공개 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String devicePublicId) {

        deviceService.deleteDevice(userDetails.getId(), devicePublicId);
        return BaseResponse.success();
    }

}