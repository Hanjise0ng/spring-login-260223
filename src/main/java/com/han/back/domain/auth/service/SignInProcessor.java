package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.event.NewDeviceLoginEvent;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignInProcessor {

    private final DeviceService deviceService;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public SignInResult execute(CustomUserDetails userDetails, DeviceInfo deviceInfo,
                                AuthToken previousTokens) {
        Long userId = userDetails.getId();
        invalidatePreviousSessionIfPresent(userId, previousTokens);

        DeviceRegistration registration = deviceService.registerLoginDevice(userId, deviceInfo);
        AuthToken tokens = tokenService.issueTokens(userId, userDetails.getRole(),
                registration.getSessionId());

        log.info("Login Success - UserPK: {} | Role: {} | SessionId: {} | DeviceType: {}",
                userId, userDetails.getRole().name(),
                registration.getSessionId(), deviceInfo.getDeviceType().name());

        if (registration.isNewDevice()) {
            eventPublisher.publishEvent(
                    NewDeviceLoginEvent.of(
                            userId,
                            userDetails.getEmail(),
                            userDetails.getNickname(),
                            deviceInfo
                    )
            );
        }

        return SignInResult.of(tokens, deviceInfo.getDeviceFingerprint());
    }

    private void invalidatePreviousSessionIfPresent(Long userId, AuthToken previousTokens) {
        if (previousTokens == null || previousTokens.isEmpty()) return;

        tokenService
                .extractUserFromTokens(previousTokens.getAccessToken(), previousTokens.getRefreshToken())
                .filter(prev -> prev.getSessionId() != null)
                .ifPresent(prev -> {
                    tokenService.invalidateSession(userId, prev.getSessionId());
                    log.debug("Previous session invalidated on re-login - UserPK: {} | OldSessionId: {}",
                            userId, prev.getSessionId());
                });
    }

}