package com.han.back.global.security.logout;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.Role;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import com.han.back.global.util.SecurityPathConst;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomLogoutHandler")
class CustomLogoutHandlerTest {

    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    private static final CustomUserDetails AT_USER = CustomUserDetails.fromToken(1L, Role.USER, "at-session");
    private static final CustomUserDetails RT_USER = CustomUserDetails.fromToken(2L, Role.USER, "rt-session");

    @Mock private TokenService tokenService;
    @Mock private DeviceService deviceService;
    @Mock private TokenTransportResolver tokenTransportResolver;
    @Mock private TokenTransport tokenTransport;

    @InjectMocks
    private CustomLogoutHandler logoutHandler;

    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @BeforeEach
    void setUp() {
        given(tokenTransportResolver.resolve(any())).willReturn(tokenTransport);
    }

    private static MockHttpServletRequest logoutRequest() {
        return new MockHttpServletRequest("POST", SecurityPathConst.LOGOUT_PATH);
    }

    private static void withAccessToken(MockHttpServletRequest request) {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);
    }

    private static void withRefreshTokenCookie(MockHttpServletRequest request) {
        request.setCookies(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, REFRESH_TOKEN));
    }

    private void givenInvalidAccessToken() {
        given(tokenService.authenticateAccessToken(ACCESS_TOKEN))
                .willThrow(new CustomAuthenticationException(AuthResponseStatus.AUTH_ACCESS_TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("유효한 AT가 있으면 AT의 사용자와 세션으로 로그아웃하고 RT는 사용하지 않는다")
    void validAccessToken_logsOutWithAtUser() {
        MockHttpServletRequest request = logoutRequest();
        withAccessToken(request);
        withRefreshTokenCookie(request);
        given(tokenService.authenticateAccessToken(ACCESS_TOKEN)).willReturn(AT_USER);

        logoutHandler.logout(request, response, null);

        then(tokenService).should().invalidateSession(1L, "at-session");
        then(deviceService).should().deactivateSession(1L, "at-session");
        then(tokenTransport).should().clear(response);
        then(tokenService).should(never()).extractUserFromRefreshToken(anyString());
        assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.SUCCESS);
    }

    @Test
    @DisplayName("AT 검증에 실패하면 RT의 사용자와 세션만으로 로그아웃한다")
    void invalidAccessToken_fallsBackToRefreshTokenUser() {
        MockHttpServletRequest request = logoutRequest();
        withAccessToken(request);
        withRefreshTokenCookie(request);
        givenInvalidAccessToken();
        given(tokenService.extractUserFromRefreshToken(REFRESH_TOKEN)).willReturn(Optional.of(RT_USER));

        logoutHandler.logout(request, response, null);

        then(tokenService).should().invalidateSession(2L, "rt-session");
        then(deviceService).should().deactivateSession(2L, "rt-session");
        assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.SUCCESS);
    }

    @Test
    @DisplayName("AT 없이 RT 쿠키만 있어도 RT의 사용자로 로그아웃한다")
    void refreshTokenOnly_logsOutWithRtUser() {
        MockHttpServletRequest request = logoutRequest();
        withRefreshTokenCookie(request);
        given(tokenService.extractUserFromRefreshToken(REFRESH_TOKEN)).willReturn(Optional.of(RT_USER));

        logoutHandler.logout(request, response, null);

        then(tokenService).should(never()).authenticateAccessToken(anyString());
        then(tokenService).should().invalidateSession(2L, "rt-session");
        assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.SUCCESS);
    }

    @Test
    @DisplayName("AT도 RT도 없으면 세션을 건드리지 않고 미인증으로 처리한다")
    void noTokens_unauthenticated() {
        MockHttpServletRequest request = logoutRequest();

        logoutHandler.logout(request, response, null);

        then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        then(deviceService).should(never()).deactivateSession(anyLong(), anyString());
        assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("AT가 무효하고 RT도 해석할 수 없으면 미인증으로 처리한다")
    void invalidAccessToken_unreadableRefreshToken_unauthenticated() {
        MockHttpServletRequest request = logoutRequest();
        withAccessToken(request);
        withRefreshTokenCookie(request);
        givenInvalidAccessToken();
        given(tokenService.extractUserFromRefreshToken(REFRESH_TOKEN)).willReturn(Optional.empty());

        logoutHandler.logout(request, response, null);

        then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("세션 무효화 중 Redis 장애가 나면 REDIS_ERROR로 처리한다")
    void redisFailure_redisError() {
        MockHttpServletRequest request = logoutRequest();
        withAccessToken(request);
        given(tokenService.authenticateAccessToken(ACCESS_TOKEN)).willReturn(AT_USER);
        willThrow(new CustomException(ResponseStatus.REDIS_ERROR))
                .given(tokenService).invalidateSession(1L, "at-session");

        logoutHandler.logout(request, response, null);

        assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.REDIS_ERROR);
    }

}