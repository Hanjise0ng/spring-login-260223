package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.service.SocialLinkStateCache;
import com.han.back.global.exception.CustomException;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.global.security.token.util.SocialLinkTokenUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("LinkAwareAuthorizationRequestResolver")
class LinkAwareAuthorizationRequestResolverTest {

    private SocialLinkTokenUtil socialLinkTokenUtil;
    private SocialLinkStateCache socialLinkStateCache;
    private LinkAwareAuthorizationRequestResolver resolver;

    @BeforeEach
    void setUp() {
        socialLinkTokenUtil = mock(SocialLinkTokenUtil.class);
        socialLinkStateCache = mock(SocialLinkStateCache.class);

        ClientRegistration kakaoLink = ClientRegistration.withRegistrationId("kakao-link")
                .clientId("test-client-id")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/kakao-link")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .scope("profile_nickname")
                .build();

        ClientRegistrationRepository repository = new InMemoryClientRegistrationRepository(kakaoLink);
        resolver = new LinkAwareAuthorizationRequestResolver(repository, socialLinkTokenUtil, socialLinkStateCache);
    }

    private MockHttpServletRequest authorizationRequest(String registrationId) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uri = "/oauth2/authorization/" + registrationId;
        request.setMethod("GET");
        request.setRequestURI(uri);
        request.setServletPath(uri);
        return request;
    }

    @Test
    @DisplayName("link_token이 있으면 검증 후 state를 키로 컨텍스트를 저장한다")
    void withLinkToken_savesContext() {
        MockHttpServletRequest request = authorizationRequest("kakao-link");
        request.setParameter(OAuth2Const.PARAM_LINK_TOKEN, "link-token");
        given(socialLinkTokenUtil.validate("link-token")).willReturn(7L);

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertThat(result).isNotNull();
        verify(socialLinkStateCache).save(eq(result.getState()), eq(7L));
    }

    @Test
    @DisplayName("link_token이 없으면 컨텍스트를 저장하지 않는다 (로그인 흐름)")
    void withoutLinkToken_noSave() {
        MockHttpServletRequest request = authorizationRequest("kakao-link");

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertThat(result).isNotNull();
        verify(socialLinkStateCache, never()).save(any(), any());
    }

    @Test
    @DisplayName("OAuth 시작 경로가 아니면 null을 반환하고 컨텍스트를 저장하지 않는다")
    void notAuthorizationPath_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setRequestURI("/some/other/path");
        request.setServletPath("/some/other/path");

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertThat(result).isNull();
        verify(socialLinkStateCache, never()).save(any(), any());
    }

    @Test
    @DisplayName("link_token이 위조면 검증 단계에서 예외가 전파된다")
    void invalidLinkToken_propagatesException() {
        MockHttpServletRequest request = authorizationRequest("kakao-link");
        request.setParameter(OAuth2Const.PARAM_LINK_TOKEN, "forged");
        given(socialLinkTokenUtil.validate("forged"))
                .willThrow(new CustomException(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID));

        assertThatThrownBy(() -> resolver.resolve(request))
                .isInstanceOf(CustomException.class);

        verify(socialLinkStateCache, never()).save(any(), any());
    }

}