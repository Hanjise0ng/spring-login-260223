package com.han.back.domain.user.service.implement;

import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.service.AccountExistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountExistencePortImpl implements AccountExistencePort {

    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;

    @Override
    public boolean existsLocalAccountByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .map(userId -> credentialRepository
                        .findByUserIdAndProvider(userId, AuthProvider.LOCAL).isPresent())
                .orElse(false);
    }

}