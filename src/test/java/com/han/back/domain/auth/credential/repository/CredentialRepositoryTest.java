package com.han.back.domain.auth.credential.repository;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.user.entity.AuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class CredentialRepositoryTest {

    @Autowired
    private CredentialRepository credentialRepository;

    @Test
    @DisplayName("같은 provider + identifier 조합은 두 번 저장할 수 없다")
    void duplicateProviderIdentifierIsRejected() {
        credentialRepository.saveAndFlush(localCredential(1L, "han123"));

        assertThatThrownBy(() ->
                credentialRepository.saveAndFlush(localCredential(2L, "han123")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 사용자가 같은 provider를 두 번 연동할 수 없다")
    void duplicateUserProviderIsRejected() {
        credentialRepository.saveAndFlush(kakaoCredential(1L, "kakao-1"));

        assertThatThrownBy(() ->
                credentialRepository.saveAndFlush(kakaoCredential(1L, "kakao-2")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 identifier라도 provider가 다르면 공존한다")
    void sameIdentifierDifferentProviderCoexists() {
        credentialRepository.saveAndFlush(localCredential(1L, "duplicate-value"));
        credentialRepository.saveAndFlush(kakaoCredential(2L, "duplicate-value"));

        assertThat(credentialRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("소셜 수단은 password가 null이어도 저장된다")
    void socialCredentialAllowsNullPassword() {
        CredentialEntity saved = credentialRepository.saveAndFlush(kakaoCredential(1L, "kakao-1"));

        assertThat(saved.getPassword()).isNull();
        assertThat(saved.isLocal()).isFalse();
    }

    private CredentialEntity localCredential(Long userId, String identifier) {
        return CredentialEntity.builder()
                .userId(userId)
                .provider(AuthProvider.LOCAL)
                .identifier(identifier)
                .password("$2a$10$encodedPasswordForTest")
                .build();
    }

    private CredentialEntity kakaoCredential(Long userId, String identifier) {
        return CredentialEntity.builder()
                .userId(userId)
                .provider(AuthProvider.KAKAO)
                .identifier(identifier)
                .build();
    }

}