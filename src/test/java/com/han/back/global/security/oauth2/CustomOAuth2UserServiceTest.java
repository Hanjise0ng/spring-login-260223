package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserResolver;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.exception.CustomException;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("CustomOAuth2UserService")
class CustomOAuth2UserServiceTest {

    private static final String REGISTRATION_ID = "kakao";

    private OAuth2UserResolver oauth2UserResolver;
    private DefaultOAuth2UserService defaultOAuth2UserService;
    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        oauth2UserResolver = mock(OAuth2UserResolver.class);
        defaultOAuth2UserService = mock(DefaultOAuth2UserService.class);
        service = new CustomOAuth2UserService(oauth2UserResolver, defaultOAuth2UserService);
    }

    private OAuth2UserRequest userRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("test-client-id")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/kakao")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .scope("profile_nickname")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600));

        return new OAuth2UserRequest(registration, accessToken);
    }

    private OAuth2User providerUser() {
        return new DefaultOAuth2User(
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", 1234567890L, "kakao_account", Map.of()),
                "id");
    }

    private OAuth2UserInfo userInfo() {
        return mock(OAuth2UserInfo.class);
    }

    @Test
    @DisplayName("표준화된 userInfo를 attribute에 심어 success handler가 읽을 수 있게 한다")
    void loadUser_attachesResolvedUserInfo() {
        OAuth2UserRequest request = userRequest();
        OAuth2User providerUser = providerUser();
        OAuth2UserInfo info = userInfo();

        given(defaultOAuth2UserService.loadUser(request)).willReturn(providerUser);
        given(oauth2UserResolver.resolve(eq(REGISTRATION_ID), any())).willReturn(info);

        OAuth2User result = service.loadUser(request);

        assertThat(result.getAttributes()).containsKey(OAuth2Const.ATTR_USER_INFO);
        assertThat(result.getAttributes().get(OAuth2Const.ATTR_USER_INFO)).isSameAs(info);
    }

    @Test
    @DisplayName("provider가 내려준 원본 attribute를 보존하면서 userInfo만 추가한다")
    void loadUser_preservesOriginalAttributes() {
        OAuth2UserRequest request = userRequest();
        OAuth2User providerUser = providerUser();

        given(defaultOAuth2UserService.loadUser(request)).willReturn(providerUser);
        given(oauth2UserResolver.resolve(eq(REGISTRATION_ID), any())).willReturn(userInfo());

        OAuth2User result = service.loadUser(request);

        assertThat(result.getAttributes()).containsKey("id");
        assertThat(result.getAttributes()).containsKey("kakao_account");
    }

    @Test
    @DisplayName("registrationId 기준으로 적절한 어댑터를 선택하도록 resolver에 위임한다")
    void loadUser_delegatesResolutionByRegistrationId() {
        OAuth2UserRequest request = userRequest();

        given(defaultOAuth2UserService.loadUser(request)).willReturn(providerUser());
        given(oauth2UserResolver.resolve(eq(REGISTRATION_ID), any())).willReturn(userInfo());

        service.loadUser(request);

        org.mockito.Mockito.verify(oauth2UserResolver).resolve(eq(REGISTRATION_ID), any());
    }

    @Test
    @DisplayName("지원하지 않는 provider면 resolver 예외가 그대로 전파된다")
    void loadUser_unsupportedProvider_propagatesException() {
        OAuth2UserRequest request = userRequest();

        given(defaultOAuth2UserService.loadUser(request)).willReturn(providerUser());
        given(oauth2UserResolver.resolve(eq(REGISTRATION_ID), any()))
                .willThrow(new CustomException(SocialResponseStatus.SOCIAL_PROVIDER_UNSUPPORTED));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("provider userInfo 조회 자체가 실패하면 그 예외가 전파된다")
    void loadUser_providerCallFails_propagatesException() {
        OAuth2UserRequest request = userRequest();

        given(defaultOAuth2UserService.loadUser(request))
                .willThrow(new org.springframework.security.oauth2.core.OAuth2AuthenticationException("server_error"));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
    }

}