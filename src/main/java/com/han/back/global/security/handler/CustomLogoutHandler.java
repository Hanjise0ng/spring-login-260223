package com.han.back.global.security.handler;

import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.AuthHttpUtil;
import com.han.back.global.security.util.CookieUtil;
import com.han.back.global.security.util.JwtUtil;
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
    private final JwtUtil jwtUtil;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {

        String accessToken = AuthHttpUtil.extractAccessToken(request).orElse("");
        String refreshToken = AuthHttpUtil.extractRefreshToken(request).orElse("");
        String userPk = resolveUserPk(accessToken, refreshToken);

        try {
            invalidateIfPresent(accessToken, refreshToken);
            clearRefreshCookie(response);
            log.info("Logout Success - UserPK: {} | ClientIP: {}",
                    userPk, request.getRemoteAddr());

        } catch (CustomAuthenticationException e) {
            // 토큰 위조·만료 → 사용 불가한 토큰, 멱등성 보장
            // 클라이언트 입장에서 로그아웃 완료 상태이므로 쿠키 삭제
            clearRefreshCookie(response);
            log.warn("Logout with invalid token (treated as success) - UserPK: {} | Reason: {}",
                    userPk, e.getMessage());

        } catch (CustomException e) {
            // Redis 장애 등 인프라 오류 → 진짜 실패
            // 쿠키 미삭제: 클라이언트가 토큰을 유지해 재시도 가능하도록 보장
            // response 직접 write 금지: LogoutSuccessHandler에 결과 위임
            LogoutContext.setResult(request, LogoutContext.Result.REDIS_ERROR);
            log.error("Logout Failed - UserPK: {} | Reason: {}",
                    userPk, e.getMessage(), e);
        }
    }

    // 토큰 존재 시에만 무효화, 없으면 이미 로그아웃된 상태로 간주 (멱등성)
    private void invalidateIfPresent(String accessToken, String refreshToken) {
        AuthTokenDto tokens = AuthTokenDto.of(accessToken, refreshToken);
        if (tokens.isEmpty()) return;
        tokenService.invalidateTokens(tokens);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        CookieUtil.addSecureCookie(response, AuthConst.COOKIE_REFRESH_TOKEN_NAME, "", 0);
    }

    // 토큰에서 직접 추출
    private String resolveUserPk(String accessToken, String refreshToken) {
        return jwtUtil.extractUserPk(accessToken)
                .or(() -> jwtUtil.extractUserPk(refreshToken))
                .orElse("UNKNOWN");
    }

}