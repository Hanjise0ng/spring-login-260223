package com.han.back.global.security.principal;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new UsernameNotFoundException(
                        AuthResponseStatus.AUTH_SIGN_IN_FAIL.getMessage()));

        if (user.isSocialUser()) {
            throw new UsernameNotFoundException(AuthResponseStatus.AUTH_SIGN_IN_FAIL.getMessage());
        }

        return new CustomUserDetails(
                user.getId(),
                user.getPassword(),
                user.getRole(),
                user.getEmail(),
                user.getNickname()
        );
    }

}