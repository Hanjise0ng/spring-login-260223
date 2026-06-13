package com.han.back.global.security.principal;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        CredentialEntity credential = credentialRepository
                .findByProviderAndIdentifier(AuthProvider.LOCAL, loginId)
                .orElseThrow(() -> {
                    log.info("Login failed - no LOCAL credential for identifier");
                    return new UsernameNotFoundException(AuthResponseStatus.AUTH_SIGN_IN_FAIL.getMessage());
                });

        UserEntity user = userRepository.findById(credential.getUserId())
                .orElseThrow(() -> {
                    log.warn("Login failed - credential references missing user - userId: {}", credential.getUserId());
                    return new UsernameNotFoundException(AuthResponseStatus.AUTH_SIGN_IN_FAIL.getMessage());
                });

        if (user.isSocialUser()) {
            log.warn("Login failed - social-only account attempted local login");
            throw new UsernameNotFoundException(AuthResponseStatus.AUTH_SIGN_IN_FAIL.getMessage());
        }

        if (user.isDeleted()) {
            log.info("Login attempt on soft-deleted account - recovery guidance follows on password match");
        }

        return CustomUserDetails.forLocalLogin(
                user.getId(),
                credential.getPassword(),
                user.getRole(),
                user.getEmail(),
                user.getNickname(),
                user.isDeleted()
        );
    }

}