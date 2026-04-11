package com.han.back.domain.auth.dto.response;

import com.han.back.domain.device.entity.DeviceType;
import com.han.back.global.security.token.AuthToken;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ReissueResponseDto {

    private final AuthToken authToken;
    private final DeviceType deviceType;

    public static ReissueResponseDto of(AuthToken authToken, DeviceType deviceType) {
        return new ReissueResponseDto(authToken, deviceType);
    }
}