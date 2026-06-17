package com.han.back.domain.auth.credential.service;

import com.han.back.domain.auth.credential.dto.request.LocalCredentialCreateRequestDto;
import com.han.back.domain.auth.credential.dto.response.LinkedCredentialResponseDto;
import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.exception.CredentialResponseStatus;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.auth.credential.service.implement.CredentialLinkServiceImpl;
import com.han.back.domain.auth.factory.UserFactory;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.token.util.LoginIdTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CredentialLinkServiceImpl")
class CredentialLinkServiceImplTest {

    @Mock private CredentialRepository credentialRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserFactory userFactory;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private VerificationService verificationService;
    @Mock private LoginIdTokenUtil loginIdTokenUtil;

    @InjectMocks private CredentialLinkServiceImpl service;

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("unlinkSocialCredential() — 소셜 연동 해제")
    class Unlink {

        @Test
        @DisplayName("로컬 계정에 연동된 소셜은 해제된다")
        void localAccount_unlinks() {
            CredentialEntity kakao = CredentialEntity.builder()
                    .userId(USER_ID).provider(AuthProvider.KAKAO).identifier("k-1").build();
            given(credentialRepository.existsByUserIdAndProvider(USER_ID, AuthProvider.LOCAL)).willReturn(true);
            given(credentialRepository.findByUserIdAndProvider(USER_ID, AuthProvider.KAKAO))
                    .willReturn(Optional.of(kakao));

            service.unlinkSocialCredential(USER_ID, AuthProvider.KAKAO);

            verify(credentialRepository).delete(kakao);
        }

        @Test
        @DisplayName("소셜 단독 계정은 해제할 수 없다")
        void socialOnly_cannotUnlink() {
            given(credentialRepository.existsByUserIdAndProvider(USER_ID, AuthProvider.LOCAL)).willReturn(false);

            assertThatThrownBy(() -> service.unlinkSocialCredential(USER_ID, AuthProvider.KAKAO))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(CredentialResponseStatus.CREDENTIAL_SOCIAL_ONLY_ACCOUNT);

            verify(credentialRepository, never()).delete(any());
        }

        @Test
        @DisplayName("연동되지 않은 소셜 해제 시 NOT_LINKED")
        void notLinked_throws() {
            given(credentialRepository.existsByUserIdAndProvider(USER_ID, AuthProvider.LOCAL)).willReturn(true);
            given(credentialRepository.findByUserIdAndProvider(USER_ID, AuthProvider.KAKAO))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.unlinkSocialCredential(USER_ID, AuthProvider.KAKAO))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(CredentialResponseStatus.CREDENTIAL_NOT_LINKED);
        }

        @Test
        @DisplayName("LOCAL 제공자 해제 시도는 PROVIDER_NOT_SOCIAL")
        void localProvider_throws() {
            assertThatThrownBy(() -> service.unlinkSocialCredential(USER_ID, AuthProvider.LOCAL))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(CredentialResponseStatus.CREDENTIAL_PROVIDER_NOT_SOCIAL);
        }
    }

    @Nested
    @DisplayName("getLinkedSocials() — 연동 목록")
    class GetLinked {

        @Test
        @DisplayName("LOCAL 본체는 제외하고 소셜만 반환한다")
        void excludesLocal() {
            given(credentialRepository.findAllByUserId(USER_ID)).willReturn(List.of(
                    CredentialEntity.builder().userId(USER_ID).provider(AuthProvider.LOCAL).identifier("id").build(),
                    CredentialEntity.builder().userId(USER_ID).provider(AuthProvider.KAKAO).identifier("k").build(),
                    CredentialEntity.builder().userId(USER_ID).provider(AuthProvider.GOOGLE).identifier("g").build()
            ));

            List<LinkedCredentialResponseDto> result = service.getLinkedSocials(USER_ID);

            assertThat(result).extracting(LinkedCredentialResponseDto::getProvider)
                    .containsExactlyInAnyOrder(AuthProvider.KAKAO, AuthProvider.GOOGLE)
                    .doesNotContain(AuthProvider.LOCAL);
        }
    }

    @Nested
    @DisplayName("promoteToLocalAccount() — 소셜단독 → 로컬계정 승격")
    class Promote {

        private LocalCredentialCreateRequestDto request() {
            return new LocalCredentialCreateRequestDto("newlogin", "Test1234!", "u@test.com", "id-token");
        }

        @Test
        @DisplayName("같은 userId에 LOCAL credential을 추가하고 이메일을 갱신한다")
        void promotesKeepingSameUserId() {
            UserEntity user = UserEntity.builder().nickname("nick").email("old@test.com").tag("0001").build();
            given(credentialRepository.existsByUserIdAndProvider(USER_ID, AuthProvider.LOCAL)).willReturn(false);
            given(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, "newlogin")).willReturn(false);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(passwordEncoder.encode("Test1234!")).willReturn("encoded");
            given(userFactory.createLocalCredential(eq(USER_ID), eq("newlogin"), eq("encoded")))
                    .willReturn(CredentialEntity.builder().userId(USER_ID).build());

            service.promoteToLocalAccount(USER_ID, request());

            verify(loginIdTokenUtil).validate("newlogin", "id-token");
            verify(verificationService).validateConfirmed("u@test.com", VerificationType.SIGN_UP);
            verify(credentialRepository).save(any(CredentialEntity.class));
            assertThat(user.getEmail()).isEqualTo("u@test.com");
        }

        @Test
        @DisplayName("이미 LOCAL이 있으면LOCAL_ALREADY_EXISTS")
        void alreadyLocal_throws() {
            given(credentialRepository.existsByUserIdAndProvider(USER_ID, AuthProvider.LOCAL)).willReturn(true);

            assertThatThrownBy(() -> service.promoteToLocalAccount(USER_ID, request()))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(CredentialResponseStatus.CREDENTIAL_LOCAL_ALREADY_EXISTS);

            verify(credentialRepository, never()).save(any());
        }
    }

}