package com.han.back.global.security.handler;

import com.han.back.domain.device.service.DeviceService;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.context.LogoutContext;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthHttpUtil;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.transport.TokenTransportResolver;
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
        Optional<CustomUserDetails> userDetails = resolveUser(authentication, request);

        if (userDetails.isEmpty()) {
            log.warn("Logout Failed - Unable to identify user | ClientIP: {}", request.getRemoteAddr());
            LogoutContext.setResult(request, LogoutContext.Result.UNAUTHENTICATED);
            return;
        }

        CustomUserDetails user = userDetails.get();

        try {
            tokenService.invalidateSession(user.getId(), user.getSessionId());
            deviceService.deactivateSession(user.getId(), user.getSessionId());
            tokenTransportResolver.resolve(request).clear(response);

            LogoutContext.setResult(request, LogoutContext.Result.SUCCESS);
            log.info("Logout Success - UserPK: {} | SessionId: {} | ClientIP: {}",
                    user.getId(), user.getSessionId(), request.getRemoteAddr());

        } catch (CustomException e) { // Redis 장애 또는 DB 오류
            LogoutContext.setResult(request, LogoutContext.Result.REDIS_ERROR);
            log.error("Logout Failed - UserPK: {} | Reason: {} | ClientIP: {}",
                    user.getId(), e.getMessage(), request.getRemoteAddr(), e);
        }
    }

    private Optional<CustomUserDetails> resolveUser(Authentication authentication, HttpServletRequest request) {
        // JwtFilter가 AT 검증 성공한 경우
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.of(userDetails);
        }

        // AT 만료 상태에서의 로그아웃
        log.debug("SecurityContext authentication unavailable - attempting RT fallback | ClientIP: {}",
                request.getRemoteAddr());

        AuthToken tokens = AuthHttpUtil.extractTokenPairLeniently(request);
        return tokenService.extractUserFromTokens(tokens.getAccessToken(), tokens.getRefreshToken());
    }

}