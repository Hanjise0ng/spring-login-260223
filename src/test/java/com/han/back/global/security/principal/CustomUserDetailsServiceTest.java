package com.han.back.global.security.principal;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock private CredentialRepository credentialRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    private static final String LOGIN_ID = "testuser";
    private static final String ENCODED_PW = "$2a$10$encoded";

    @Test
    @DisplayName("LOCAL credential과 user가 존재하면 UserDetails를 반환한다")
    void loadsUserDetailsWhenLocalCredentialExists() {
        CredentialEntity credential = CredentialEntity.builder()
                .userId(1L).provider(AuthProvider.LOCAL)
                .identifier(LOGIN_ID).password(ENCODED_PW).build();
        UserEntity user = UserEntity.builder()
                .email("t@test.com").nickname("u").tag("A1B2")
                .role(Role.USER).authProvider(AuthProvider.LOCAL).build();

        given(credentialRepository.findByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                .willReturn(Optional.of(credential));
        given(userRepository.findById(credential.getUserId())).willReturn(Optional.of(user));

        UserDetails result = customUserDetailsService.loadUserByUsername(LOGIN_ID);

        assertThat(result.getPassword()).isEqualTo(ENCODED_PW);
    }

    @Test
    @DisplayName("LOCAL credential이 없으면(소셜전용·미존재) UsernameNotFoundException")
    void throwsWhenNoLocalCredential() {
        given(credentialRepository.findByProviderAndIdentifier(AuthProvider.LOCAL, "nonexistent"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("credential은 있으나 user가 없으면 UsernameNotFoundException")
    void throwsWhenUserMissing() {
        CredentialEntity credential = CredentialEntity.builder()
                .userId(99L).provider(AuthProvider.LOCAL)
                .identifier(LOGIN_ID).password(ENCODED_PW).build();
        given(credentialRepository.findByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                .willReturn(Optional.of(credential));
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(LOGIN_ID))
                .isInstanceOf(UsernameNotFoundException.class);
    }

}