package com.han.back.global.security.handler;

import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.AuthHttpUtil;
import com.han.back.global.security.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final TokenService tokenService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            log.error("Logout reached with unexpected principal type: {} | ClientIP: {}",
                    authentication.getPrincipal().getClass().getSimpleName(), request.getRemoteAddr());
            LogoutContext.setResult(request, LogoutContext.Result.UNAUTHENTICATED);
            return;
        }

        String accessToken = AuthHttpUtil.extractAccessToken(request).orElse("");
        String refreshToken = AuthHttpUtil.extractRefreshToken(request).orElse("");

        try {
            invalidateIfPresent(accessToken, refreshToken);
            clearRefreshCookie(response);
            log.info("Logout Success - UserPK: {} | ClientIP: {}",
                    userDetails.getId(), request.getRemoteAddr());

        } catch (CustomException e) { // Redis 장애 — 쿠키 미삭제, 클라이언트 재시도 보장
            LogoutContext.setResult(request, LogoutContext.Result.REDIS_ERROR);
            log.error("Logout Failed - UserPK: {} | Reason: {} | ClientIP: {}",
                    userDetails.getId(), e.getMessage(), request.getRemoteAddr(), e);
        }
    }

    private void invalidateIfPresent(String accessToken, String refreshToken) {
        AuthTokenDto tokens = AuthTokenDto.of(accessToken, refreshToken);
        if (tokens.isEmpty()) return;
        tokenService.invalidateTokens(tokens);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        CookieUtil.addSecureCookie(response, AuthConst.COOKIE_REFRESH_TOKEN_NAME, "", 0);
    }

}