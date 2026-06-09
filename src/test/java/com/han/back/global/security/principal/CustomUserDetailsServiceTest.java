package com.han.back.global.security.principal;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.fixture.UserFixture;
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

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("소셜 유저 loginId로 로드 시 UsernameNotFoundException (SF 메시지)")
    void socialUser_throwsUsernameNotFoundException() {
        UserEntity socialUser = UserFixture.socialUser();
        given(userRepository.findByLoginId(socialUser.getLoginId()))
                .willReturn(Optional.of(socialUser));

        assertThatThrownBy(() ->
                customUserDetailsService.loadUserByUsername(socialUser.getLoginId()))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(AuthResponseStatus.AUTH_SIGN_IN_FAIL.getMessage());
    }

    @Test
    @DisplayName("로컬 유저 → CustomUserDetails 정상 반환")
    void localUser_returnsCustomUserDetails() {
        UserEntity localUser = UserFixture.localUser();
        given(userRepository.findByLoginId(localUser.getLoginId()))
                .willReturn(Optional.of(localUser));

        UserDetails result = customUserDetailsService.loadUserByUsername(localUser.getLoginId());

        assertThat(result).isInstanceOf(CustomUserDetails.class);
    }

    @Test
    @DisplayName("미존재 loginId → SF와 동일한 메시지")
    void notFound_sameMessageAsSocialGuard() {
        given(userRepository.findByLoginId("nonexistent")).willReturn(Optional.empty());

        assertThatThrownBy(() ->
                customUserDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(AuthResponseStatus.AUTH_SIGN_IN_FAIL.getMessage());
    }

}