package com.han.back.integration;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.factory.UserFactory;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.fixture.DeviceFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class SocialLoginCredentialTest extends IntegrationTestBase {

    @Autowired private AuthService authService;
    @Autowired private UserFactory userFactory;

    private static final String PROVIDER_ID = "kakao-9876543210";
    private static final String SOCIAL_EMAIL = "social@test.com";
    private static final String NICKNAME = "소셜유저";

    private DeviceInfo deviceInfo() {
        return DeviceFixture.webDeviceInfo();
    }

    private OAuth2UserInfo userInfo(String email) {
        return new OAuth2UserInfo() {
            @Override public AuthProvider getProvider() { return AuthProvider.KAKAO; }
            @Override public String getProviderId() { return PROVIDER_ID; }
            @Override public String getEmail() { return email; }
            @Override public String getNickname() { return NICKNAME; }
        };
    }

    @Test
    @DisplayName("신규 소셜 로그인 시 users와 소셜 credential이 생성된다")
    void newSocialLoginCreatesUserAndCredential() {
        SocialSignInResult result = authService.processSocialLogin(userInfo(SOCIAL_EMAIL), deviceInfo());

        assertThat(result).isInstanceOf(SocialSignInResult.Authenticated.class);

        CredentialEntity credential = credentialRepository
                .findByProviderAndIdentifier(AuthProvider.KAKAO, PROVIDER_ID)
                .orElseThrow();
        assertThat(credential.getPassword()).isNull();

        UserEntity user = userRepository.findById(credential.getUserId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(SOCIAL_EMAIL);
    }

    @Test
    @DisplayName("기존 소셜 credential이 있으면 신규 가입 없이 재로그인한다")
    void existingCredentialResultsInRelogin() {
        UserEntity user = userRepository.save(userFactory.createSocialUser(NICKNAME, SOCIAL_EMAIL, "S001"));
        credentialRepository.save(userFactory.createSocialCredential(user.getId(), AuthProvider.KAKAO, PROVIDER_ID));
        long userCountBefore = userRepository.count();

        SocialSignInResult result = authService.processSocialLogin(userInfo(SOCIAL_EMAIL), deviceInfo());

        assertThat(result).isInstanceOf(SocialSignInResult.Authenticated.class);
        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }

    @Test
    @DisplayName("provider 이메일이 없으면 EmailRequired를 반환한다")
    void noEmailReturnsEmailRequired() {
        SocialSignInResult result = authService.processSocialLogin(userInfo(null), deviceInfo());

        assertThat(result).isInstanceOf(SocialSignInResult.EmailRequired.class);
        assertThat(credentialRepository.findByProviderAndIdentifier(AuthProvider.KAKAO, PROVIDER_ID)).isEmpty();
    }

}