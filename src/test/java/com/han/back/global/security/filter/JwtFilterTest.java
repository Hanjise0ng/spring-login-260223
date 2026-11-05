package com.han.back.global.security.filter;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.entity.Role;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.util.SecurityPathConst;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtFilter")
class JwtFilterTest {

    private static final String PROTECTED_PATH = "/api/v1/users/me";
    private static final String VALID_TOKEN = "valid-token";
    private static final String INVALID_TOKEN = "invalid-token";

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private JwtFilter jwtFilter;

    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain filterChain = new MockFilterChain();

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static MockHttpServletRequest request(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private static MockHttpServletRequest requestWithToken(String path, String token) {
        MockHttpServletRequest request = request(path);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private boolean proceededToNextFilter() {
        return filterChain.getRequest() != null;
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/api/v1/auth/sign-in", "/api/v1/auth/reissue", "/oauth2/authorization/kakao"})
    @DisplayName("공개 경로는 토큰이 있어도 검사하지 않고 다음 필터로 넘긴다")
    void publicPath_skipsTokenValidation(String publicPath) throws Exception {
        jwtFilter.doFilter(requestWithToken(publicPath, INVALID_TOKEN), response, filterChain);

        verifyNoInteractions(tokenService);
        assertThat(currentAuthentication()).isNull();
        assertThat(proceededToNextFilter()).isTrue();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 정보를 설정하지 않고 다음 필터로 넘긴다")
    void noToken_proceedsWithoutAuthentication() throws Exception {
        jwtFilter.doFilter(request(PROTECTED_PATH), response, filterChain);

        verifyNoInteractions(tokenService);
        assertThat(currentAuthentication()).isNull();
        assertThat(proceededToNextFilter()).isTrue();
    }

    @Test
    @DisplayName("Bearer 형식이 아닌 Authorization 헤더는 토큰이 없는 것으로 처리한다")
    void nonBearerHeader_treatedAsNoToken() throws Exception {
        MockHttpServletRequest request = request(PROTECTED_PATH);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz");

        jwtFilter.doFilter(request, response, filterChain);

        verifyNoInteractions(tokenService);
        assertThat(currentAuthentication()).isNull();
        assertThat(proceededToNextFilter()).isTrue();
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 사용자와 권한이 설정된다")
    void validToken_setsAuthentication() throws Exception {
        CustomUserDetails userDetails = CustomUserDetails.fromToken(1L, Role.USER, "session-1");
        given(tokenService.authenticateAccessToken(VALID_TOKEN)).willReturn(userDetails);

        jwtFilter.doFilter(requestWithToken(PROTECTED_PATH, VALID_TOKEN), response, filterChain);

        Authentication authentication = currentAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isSameAs(userDetails);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly(Role.USER.getAuthority());
        assertThat(proceededToNextFilter()).isTrue();
    }

    @Test
    @DisplayName("무효한 토큰이면 예외를 전파하고 다음 필터로 넘기지 않는다")
    void invalidToken_propagatesException() {
        given(tokenService.authenticateAccessToken(INVALID_TOKEN))
                .willThrow(new CustomAuthenticationException(AuthResponseStatus.AUTH_ACCESS_TOKEN_EXPIRED));

        assertThatThrownBy(() ->
                jwtFilter.doFilter(requestWithToken(PROTECTED_PATH, INVALID_TOKEN), response, filterChain))
                .isInstanceOf(CustomAuthenticationException.class);

        assertThat(currentAuthentication()).isNull();
        assertThat(proceededToNextFilter()).isFalse();
    }

    @Test
    @DisplayName("로그아웃 경로는 토큰을 검사하지 않고 넘겨 CustomLogoutHandler가 직접 해석하게 한다")
    void logoutPath_skipsTokenValidation() throws Exception {
        jwtFilter.doFilter(requestWithToken(SecurityPathConst.LOGOUT_PATH, INVALID_TOKEN), response, filterChain);

        verifyNoInteractions(tokenService);
        assertThat(currentAuthentication()).isNull();
        assertThat(proceededToNextFilter()).isTrue();
    }

}