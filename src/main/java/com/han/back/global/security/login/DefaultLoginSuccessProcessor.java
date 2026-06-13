package com.han.back.global.security.login;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.response.RecoveryGuidanceResponseDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.user.policy.WithdrawalGracePolicy;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import com.han.back.global.security.token.util.AuthHttpUtil;
import com.han.back.global.util.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultLoginSuccessProcessor implements LoginSuccessProcessor {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final DeviceInfoProvider deviceInfoProvider;
    private final TokenTransportResolver tokenTransportResolver;
    private final HttpResponseUtil httpResponseUtil;
    private final LoginFailureRateLimiter rateLimiter;

    @Override
    public void process(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        if (userDetails.isDeleted()) {
            writeRecoveryGuidance(response, userDetails.getId());
            return;
        }

        AuthToken previousTokens = AuthHttpUtil.extractTokenPairLeniently(request);
        DeviceInfo deviceInfo = deviceInfoProvider.get(request);
        SignInResult result = authService.completeSignIn(userDetails, deviceInfo, previousTokens);

        TokenTransport transport = tokenTransportResolver.resolve(request);
        transport.write(response, result.getTokens());
        transport.writeDeviceCookie(response, result.getDeviceFingerprint());

        String loginId = MDC.get("loginId");
        if (loginId != null) {
            rateLimiter.clearOnSuccess(loginId);
        }

        httpResponseUtil.writeResponse(response, ResponseStatus.SUCCESS);
    }

    private void writeRecoveryGuidance(HttpServletResponse response, Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AccountResponseStatus.ACCOUNT_USER_NOT_FOUND));

        long remainingDays = WithdrawalGracePolicy.remainingDays(user.getDeletedAt(), LocalDateTime.now());

        log.info("Recovery guidance issued - deleted account login - remainingDays: {}", remainingDays);

        httpResponseUtil.writeResponse(
                response,
                AccountResponseStatus.ACCOUNT_ALREADY_DELETED,
                RecoveryGuidanceResponseDto.of(remainingDays)
        );
    }

}