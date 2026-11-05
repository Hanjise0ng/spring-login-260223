package com.han.back.global.security.logout;

import com.han.back.domain.device.service.DeviceService;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.util.AuthHttpUtil;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import com.han.back.global.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final TokenService tokenService;
    private final DeviceService deviceService;
    private final TokenTransportResolver tokenTransportResolver;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        Optional<CustomUserDetails> userDetails = resolveUser(request);

        if (userDetails.isEmpty()) {
            log.warn("Logout Failed - Unable to identify user | ClientIP: {}", ClientIpResolver.resolve(request));
            LogoutContext.setResult(request, LogoutResult.UNAUTHENTICATED);
            return;
        }

        CustomUserDetails user = userDetails.get();

        try {
            tokenService.invalidateSession(user.getId(), user.getSessionId());
            deviceService.deactivateSession(user.getId(), user.getSessionId());
            tokenTransportResolver.resolve(request).clear(response);

            LogoutContext.setResult(request, LogoutResult.SUCCESS);
            log.info("Logout Success - UserPK: {} | SessionId: {} | ClientIP: {}",
                    user.getId(), user.getSessionId(), ClientIpResolver.resolve(request));

        } catch (CustomException e) { // Redis 장애 또는 DB 오류
            LogoutContext.setResult(request, LogoutResult.REDIS_ERROR);
            log.error("Logout Failed - UserPK: {} | Reason: {} | ClientIP: {}",
                    user.getId(), e.getMessage(), ClientIpResolver.resolve(request), e);
        }
    }

    // JwtFilter는 로그아웃 경로를 건너뛰므로 여기서 AT → RT 순서로 사용자를 직접 식별한다
    private Optional<CustomUserDetails> resolveUser(HttpServletRequest request) {
        return AuthHttpUtil.extractAccessToken(request)
                .flatMap(this::authenticateAccessTokenQuietly)
                .or(() -> resolveUserFromRefreshToken(request));
    }

    private Optional<CustomUserDetails> authenticateAccessTokenQuietly(String accessToken) {
        try {
            return Optional.of(tokenService.authenticateAccessToken(accessToken));
        } catch (CustomAuthenticationException e) { // AT 만료·블랙리스트·위조 → RT로 식별 시도
            return Optional.empty();
        }
    }

    private Optional<CustomUserDetails> resolveUserFromRefreshToken(HttpServletRequest request) {
        log.debug("Access token unavailable - attempting RT fallback | ClientIP: {}",
                ClientIpResolver.resolve(request));

        return AuthHttpUtil.extractRefreshToken(request)
                .flatMap(tokenService::extractUserFromRefreshToken);
    }

}