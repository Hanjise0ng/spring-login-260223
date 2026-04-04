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

    @GetMapping
    public ResponseEntity<BaseResponse<List<DeviceDetailResponseDto>>> getMyDevices(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<DeviceDetailResponseDto> devices = deviceService.getDevices(
                userDetails.getId(), userDetails.getSessionId()
        );
        return BaseResponse.success(devices);
    }

    @PostMapping("/{devicePublicId}/logout")
    public ResponseEntity<BaseResponse<Empty>> forceLogoutDevice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String devicePublicId) {

        deviceService.forceLogoutDevice(
                userDetails.getId(), devicePublicId, userDetails.getSessionId()
        );
        return BaseResponse.success();
    }

    @DeleteMapping("/{devicePublicId}")
    public ResponseEntity<BaseResponse<Empty>> deleteDevice(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String devicePublicId) {

        deviceService.deleteDevice(userDetails.getId(), devicePublicId);
        return BaseResponse.success();
    }

}