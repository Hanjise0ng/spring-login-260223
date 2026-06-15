package com.han.back.integration;

import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.Role;
import com.han.back.fixture.DeviceFixture;
import com.han.back.global.security.principal.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;

@DisplayName("로그인 트랜잭션 경계 — DB 롤백 시 Redis 부수효과 미발생")
class SignInTransactionBoundaryTest extends IntegrationTestBase {

    @Autowired private AuthService authService;

    @MockitoBean private DeviceService deviceService;

    @Test
    @DisplayName("디바이스 등록 실패 시 Redis에 RT가 저장되지 않는다")
    void noRefreshTokenInRedisWhenDeviceRegistrationFails() {
        Long userId = 9999L;
        CustomUserDetails userDetails =
                CustomUserDetails.forSocialLogin(userId, Role.USER, "u@test.com", "nick");
        DeviceInfo deviceInfo = DeviceFixture.webDeviceInfo();

        willThrow(new RuntimeException("device registration failed"))
                .given(deviceService).registerLoginDevice(eq(userId), any());

        assertThatThrownBy(() -> authService.completeSignIn(userDetails, deviceInfo, null))
                .isInstanceOf(RuntimeException.class);

        Set<String> rtKeys = getRtKeys(userId);
        assertThat(rtKeys).isEmpty();
    }

}