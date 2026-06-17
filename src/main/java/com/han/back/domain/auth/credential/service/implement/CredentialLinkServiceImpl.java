package com.han.back.domain.auth.credential.service.implement;

import com.han.back.domain.auth.credential.dto.response.LinkedCredentialResponseDto;
import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.exception.CredentialResponseStatus;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialLinkServiceImpl implements CredentialLinkService {

    private final CredentialRepository credentialRepository;

    @Override
    @Transactional
    public void unlinkSocialCredential(Long userId, AuthProvider provider) {
        requireSocialProvider(provider);
        requireLocalAccount(userId);

        CredentialEntity credential = credentialRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new CustomException(CredentialResponseStatus.CREDENTIAL_NOT_LINKED));

        credentialRepository.delete(credential);

        log.info("Social Unlinked - UserPK: {} | Provider: {}", userId, provider);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LinkedCredentialResponseDto> getLinkedSocials(Long userId) {
        return credentialRepository.findAllByUserId(userId).stream()
                .filter(credential -> credential.getProvider().isSocial())
                .map(LinkedCredentialResponseDto::of)
                .toList();
    }

    private void requireLocalAccount(Long userId) {
        if (!credentialRepository.existsByUserIdAndProvider(userId, AuthProvider.LOCAL)) {
            throw new CustomException(CredentialResponseStatus.CREDENTIAL_SOCIAL_ONLY_ACCOUNT);
        }
    }

    private void requireSocialProvider(AuthProvider provider) {
        if (!provider.isSocial()) {
            throw new CustomException(CredentialResponseStatus.CREDENTIAL_PROVIDER_NOT_SOCIAL);
        }
    }

}