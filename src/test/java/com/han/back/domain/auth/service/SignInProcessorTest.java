package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.Role;
import com.han.back.fixture.DeviceFixture;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthToken;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SignInProcessor — 트랜잭션 경계와 Redis 쓰기 순서")
class SignInProcessorTest {

    @Mock private DeviceService deviceService;
    @Mock private TokenService tokenService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private SignInProcessor signInProcessor;

    private static final Long USER_ID = 1L;

    private CustomUserDetails userDetails() {
        return CustomUserDetails.forSocialLogin(USER_ID, Role.USER, "u@test.com", "nick");
    }

    @Test
    @DisplayName("디바이스 등록이 실패하면 토큰 발급·세션 무효화(Redis)는 호출되지 않는다")
    void redisWritesSkippedWhenDeviceRegistrationFails() {
        DeviceInfo deviceInfo = DeviceFixture.webDeviceInfo();
        willThrow(new RuntimeException("DB error"))
                .given(deviceService).registerLoginDevice(eq(USER_ID), any());

        assertThatThrownBy(() -> signInProcessor.execute(userDetails(), deviceInfo, null))
                .isInstanceOf(RuntimeException.class);

        then(tokenService).should(never()).issueTokens(any(), any(), anyString());
        then(tokenService).should(never()).invalidateSession(any(), anyString());
    }

    @Test
    @DisplayName("디바이스 등록 커밋 이후에 토큰 발급(Redis 쓰기)이 일어난다")
    void tokenIssuedAfterDeviceRegistration() {
        DeviceInfo deviceInfo = DeviceFixture.webDeviceInfo();
        DeviceRegistration registration =
                DeviceRegistration.of("session-1", true, List.of());
        given(deviceService.registerLoginDevice(eq(USER_ID), any())).willReturn(registration);
        given(tokenService.issueTokens(eq(USER_ID), eq(Role.USER), eq("session-1")))
                .willReturn(AuthToken.of("at", "rt"));

        SignInResult result = signInProcessor.execute(userDetails(), deviceInfo, null);

        InOrder inOrder = inOrder(deviceService, tokenService);
        inOrder.verify(deviceService).registerLoginDevice(eq(USER_ID), any());
        inOrder.verify(tokenService).issueTokens(eq(USER_ID), eq(Role.USER), eq("session-1"));
        assertThat(result.getTokens().getRefreshToken()).isEqualTo("rt");
    }

    @Test
    @DisplayName("퇴출 세션 목록이 있으면 각 세션을 Redis에서 무효화한다")
    void evictedSessionsAreInvalidated() {
        DeviceInfo deviceInfo = DeviceFixture.webDeviceInfo();
        DeviceRegistration registration =
                DeviceRegistration.of("session-new", false, List.of("evicted-1", "evicted-2"));
        given(deviceService.registerLoginDevice(eq(USER_ID), any())).willReturn(registration);
        given(tokenService.issueTokens(any(), any(), anyString()))
                .willReturn(AuthToken.of("at", "rt"));

        signInProcessor.execute(userDetails(), deviceInfo, null);

        verify(tokenService).invalidateSession(USER_ID, "evicted-1");
        verify(tokenService).invalidateSession(USER_ID, "evicted-2");
    }

    @Test
    @DisplayName("신규 디바이스가 아니면 NewDeviceLoginEvent를 발행하지 않는다")
    void noEventWhenNotNewDevice() {
        DeviceInfo deviceInfo = DeviceFixture.webDeviceInfo();
        DeviceRegistration registration =
                DeviceRegistration.of("session-1", false, List.of());
        given(deviceService.registerLoginDevice(eq(USER_ID), any())).willReturn(registration);
        given(tokenService.issueTokens(any(), any(), anyString()))
                .willReturn(AuthToken.of("at", "rt"));

        signInProcessor.execute(userDetails(), deviceInfo, null);

        then(eventPublisher).should(never()).publishEvent(any());
    }

}