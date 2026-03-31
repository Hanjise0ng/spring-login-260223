package com.han.back.controller;

import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.Empty;
import com.han.back.global.security.dto.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 내 디바이스 목록 조회.
     * 현재 사용자에 등록된 모든 디바이스를 최근 로그인 순으로 반환한다.
     */
    @GetMapping
    public ResponseEntity<BaseResponse<List<DeviceDetailResponseDto>>> getMyDevices(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<DeviceDetailResponseDto> devices = deviceService.getDevices(
                userDetails.getId(), userDetails.getSessionId()
        );
        return BaseResponse.success(devices);
    }

    /**
     * 특정 디바이스 강제 로그아웃.
     * 대상 디바이스의 세션을 무효화한다.
     * 현재 접속 중인 기기는 강제 로그아웃 불가.
     */
    @PostMapping("/{devicePublicId}/logout")
    public ResponseEntity<BaseResponse<Empty>> forceLogoutDevice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String devicePublicId) {

        deviceService.forceLogoutDevice(
                userDetails.getId(), devicePublicId, userDetails.getSessionId()
        );
        return BaseResponse.success();
    }

    /**
     * 비활성 디바이스를 목록에서 제거.
     * 활성 세션이 있는 디바이스는 삭제 불가.
     */
    @DeleteMapping("/{devicePublicId}")
    public ResponseEntity<BaseResponse<Empty>> deleteDevice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String devicePublicId) {

        deviceService.deleteDevice(userDetails.getId(), devicePublicId);
        return BaseResponse.success();
    }

}