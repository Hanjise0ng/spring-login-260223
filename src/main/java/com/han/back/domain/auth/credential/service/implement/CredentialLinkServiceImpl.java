package com.han.back.domain.auth.credential.service.implement;

import com.han.back.domain.auth.credential.dto.request.LocalCredentialCreateRequestDto;
import com.han.back.domain.auth.credential.dto.response.LinkedCredentialResponseDto;
import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.exception.CredentialResponseStatus;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.auth.factory.UserFactory;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.token.util.LoginIdTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialLinkServiceImpl implements CredentialLinkService {

    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final LoginIdTokenUtil loginIdTokenUtil;

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

    @Override
    @Transactional
    public void promoteToLocalAccount(Long userId, LocalCredentialCreateRequestDto request) {
        if (credentialRepository.existsByUserIdAndProvider(userId, AuthProvider.LOCAL)) {
            throw new CustomException(CredentialResponseStatus.CREDENTIAL_LOCAL_ALREADY_EXISTS);
        }

        loginIdTokenUtil.validate(request.getLoginId(), request.getLoginIdToken());
        verificationService.validateConfirmed(request.getEmail(), VerificationType.SIGN_UP);

        if (credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, request.getLoginId())) {
            throw new CustomException(AccountResponseStatus.ACCOUNT_DUPLICATE_LOGIN_ID);
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(AccountResponseStatus.ACCOUNT_USER_NOT_FOUND));

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        CredentialEntity localCredential =
                userFactory.createLocalCredential(userId, request.getLoginId(), encodedPassword);
        credentialRepository.save(localCredential);

        user.changeEmail(request.getEmail());

        log.info("Account Promoted to Local - UserPK: {} | LoginId: {}", userId, request.getLoginId());
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