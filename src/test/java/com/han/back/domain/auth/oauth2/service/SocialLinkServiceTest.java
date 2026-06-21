package com.han.back.domain.auth.oauth2.service;

import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialLinkService")
class SocialLinkServiceTest {

    @Mock private SocialLinkStateCache socialLinkStateCache;
    @Mock private CredentialLinkService credentialLinkService;

    @InjectMocks private SocialLinkService socialLinkService;

    private static final String STATE = "state-1";

    private OAuth2UserInfo userInfo(AuthProvider provider, String providerId) {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        given(userInfo.getProvider()).willReturn(provider);
        given(userInfo.getProviderId()).willReturn(providerId);
        return userInfo;
    }

    @Test
    @DisplayName("state로 userId를 복원해 소셜 자격증명을 연동한다")
    void link_success() {
        given(socialLinkStateCache.consume(STATE)).willReturn(Optional.of(4L));
        OAuth2UserInfo userInfo = userInfo(AuthProvider.KAKAO, "kakao-123");

        socialLinkService.link(STATE, userInfo);

        verify(credentialLinkService).linkSocialCredential(4L, AuthProvider.KAKAO, "kakao-123");
    }

    @Test
    @DisplayName("연동 컨텍스트가 없으면 SOCIAL_LINK_TOKEN_INVALID 예외를 던지고 연동하지 않는다")
    void link_noContext_throws() {
        given(socialLinkStateCache.consume(STATE)).willReturn(Optional.empty());

        assertThatThrownBy(() -> socialLinkService.link(STATE, mock(OAuth2UserInfo.class)))
                .isInstanceOf(CustomException.class)
                .extracting("status")
                .isEqualTo(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID);

        verify(credentialLinkService, never()).linkSocialCredential(any(), any(), any());
    }

    @Test
    @DisplayName("연동 단계에서 발생한 예외는 그대로 전파된다")
    void link_linkFailure_propagates() {
        given(socialLinkStateCache.consume(STATE)).willReturn(Optional.of(4L));
        OAuth2UserInfo userInfo = userInfo(AuthProvider.KAKAO, "kakao-123");
        willThrow(new CustomException(SocialResponseStatus.SOCIAL_ALREADY_LINKED))
                .given(credentialLinkService).linkSocialCredential(4L, AuthProvider.KAKAO, "kakao-123");

        assertThatThrownBy(() -> socialLinkService.link(STATE, userInfo))
                .isInstanceOf(CustomException.class)
                .extracting("status")
                .isEqualTo(SocialResponseStatus.SOCIAL_ALREADY_LINKED);
    }

}