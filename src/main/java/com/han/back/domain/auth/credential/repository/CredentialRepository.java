package com.han.back.domain.auth.credential.repository;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.user.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository extends JpaRepository<CredentialEntity, Long> {

    Optional<CredentialEntity> findByProviderAndIdentifier(AuthProvider provider, String identifier);

    Optional<CredentialEntity> findByUserIdAndProvider(Long userId, AuthProvider provider);

    List<CredentialEntity> findAllByUserId(Long userId);

    boolean existsByProviderAndIdentifier(AuthProvider provider, String identifier);

    boolean existsByUserIdAndProvider(Long userId, AuthProvider provider);

}