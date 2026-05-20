package com.han.back.domain.auth.oauth2.repository;

import com.han.back.domain.auth.oauth2.entity.SocialAccountEntity;
import com.han.back.domain.user.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialAccountRepository extends JpaRepository<SocialAccountEntity, Long> {

    Optional<SocialAccountEntity> findByProviderAndProviderId(AuthProvider provider, String providerId);

    boolean existsByProviderAndProviderId(AuthProvider provider, String providerId);

}