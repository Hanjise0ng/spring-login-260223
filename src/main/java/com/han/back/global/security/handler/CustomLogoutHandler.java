package com.han.back.global.security.handler;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.AuthHttpUtil;
import com.han.back.global.security.util.CookieUtil;
import com.han.back.global.security.util.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        String accessToken = AuthHttpUtil.extractAccessToken(request);
        String refreshToken = AuthHttpUtil.extractRefreshToken(request);
        String userId = (authentication != null) ? authentication.getName() : "UNKNOWN";

        try {
            tokenService.invalidateTokens(AuthTokenDto.of(accessToken, refreshToken));
        } catch (CustomAuthenticationException e) {
            log.warn("Invalid token during logout - UserId: {} | Reason: {}", userId, e.getMessage());

        } catch (CustomException e) {
            log.error("Logout failed due to Redis error - UserId: {}", userId, e);
            HttpResponseUtil.writeResponse(response, objectMapper, BaseResponseStatus.REDIS_ERROR);
            return;
        }

        CookieUtil.addSecureCookie(response, AuthConst.COOKIE_REFRESH_TOKEN_NAME, "", 0);
        log.info("Logout Success - UserId: {} | ClientIP: {}", userId, request.getRemoteAddr());
    }

}