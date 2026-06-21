package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialSignUpResponseWriter")
class SocialSignUpResponseWriterTest {

    @Mock private TokenTransportResolver tokenTransportResolver;
    @Mock private SignUpTokenCookieManager signUpTokenCookieManager;

    @InjectMocks private SocialSignUpResponseWriter responseWriter;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @Test
    @DisplayName("Authenticated면 토큰을 쓰고 가입 토큰 쿠키를 정리한다")
    void write_authenticated() {
        TokenTransport transport = mock(TokenTransport.class);
        given(tokenTransportResolver.resolve(request)).willReturn(transport);

        SocialSignInResult.Authenticated result = mock(SocialSignInResult.Authenticated.class, RETURNS_DEEP_STUBS);
        SignInResult signInResult = mock(SignInResult.class, RETURNS_DEEP_STUBS);
        given(result.getSignInResult()).willReturn(signInResult);
        given(signInResult.getDeviceFingerprint()).willReturn("fingerprint");

        responseWriter.write(result, request, response);

        verify(transport).write(response, signInResult.getTokens());
        verify(transport).writeDeviceCookie(response, "fingerprint");
        verify(signUpTokenCookieManager).clear(response);
    }

    @Test
    @DisplayName("LinkSuggested면 토큰을 쓰지 않고 link_suggested 상태를 반환한다")
    void write_linkSuggested() {
        SocialSignInResult.LinkSuggested result = mock(SocialSignInResult.LinkSuggested.class);

        responseWriter.write(result, request, response);

        verify(tokenTransportResolver, never()).resolve(request);
        verify(signUpTokenCookieManager, never()).clear(response);
    }

    @Test
    @DisplayName("EmailRequired면 가입 완료 단계에서 올 수 없으므로 예외를 던진다")
    void write_emailRequired_throws() {
        SocialSignInResult.EmailRequired result = mock(SocialSignInResult.EmailRequired.class);

        assertThatThrownBy(() -> responseWriter.write(result, request, response))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("clearSignUpToken은 가입 토큰 쿠키를 정리한다")
    void clearSignUpToken() {
        responseWriter.clearSignUpToken(response);

        verify(signUpTokenCookieManager).clear(response);
    }

}