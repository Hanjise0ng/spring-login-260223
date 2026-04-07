package com.han.back.domain.device.dto.response;

import com.han.back.domain.device.entity.DeviceEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "디바이스 상세 정보")
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceDetailResponseDto {

    @Schema(description = "디바이스 공개 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    private final String publicId;

    @Schema(description = "디바이스 타입 코드", example = "WEB_DESKTOP")
    private final String deviceType;

    @Schema(description = "디바이스 타입 표시명", example = "데스크톱 웹")
    private final String deviceTypeName;

    @Schema(description = "운영체제", example = "Windows 10")
    private final String osName;

    @Schema(description = "브라우저", example = "Chrome 120")
    private final String browserName;

    @Schema(description = "마지막 로그인 IP", example = "192.168.1.100")
    private final String lastLoginIp;

    @Schema(description = "마지막 로그인 시각", example = "2025-01-15T09:30:00")
    private final LocalDateTime lastLoginAt;

    @Schema(description = "최초 로그인 시각", example = "2024-12-01T14:20:00")
    private final LocalDateTime firstLoginAt;

    @Schema(description = "현재 요청 디바이스 여부", example = "true")
    private final boolean currentDevice;

    @Schema(description = "활성 세션 존재 여부", example = "true")
    private final boolean activeSession;

    public static DeviceDetailResponseDto from(DeviceEntity device, String currentSessionId) {
        boolean isActive = device.hasActiveSession();
        boolean isCurrent = isActive && device.getSessionId().equals(currentSessionId);

        return DeviceDetailResponseDto.builder()
                .publicId(device.getPublicId())
                .deviceType(device.getDeviceType().name())
                .deviceTypeName(device.getDeviceType().getDisplayName())
                .osName(device.getOsName())
                .browserName(device.getBrowserName())
                .lastLoginIp(device.getLastLoginIp())
                .lastLoginAt(device.getLastLoginAt())
                .firstLoginAt(device.getCreatedAt())
                .currentDevice(isCurrent)
                .activeSession(isActive)
                .build();
    }

}