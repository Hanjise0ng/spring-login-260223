package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.auth.factory.UserFactory;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.entity.SocialAccountEntity;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.auth.oauth2.repository.SocialAccountRepository;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.auth.service.SignInProcessor;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.event.UserSignedUpEvent;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.user.service.TagGenerator;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.SocialSignUpClaims;
import com.han.back.global.security.token.util.LoginIdTokenUtil;
import com.han.back.global.security.token.util.SocialSignUpTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final UserFactory userFactory;
    private final TagGenerator tagGenerator;
    private final PasswordEncoder passwordEncoder;
    private final VerificationService verificationService;
    private final LoginIdTokenUtil loginIdTokenUtil;
    private final SocialSignUpTokenUtil socialSignUpTokenUtil;
    private final TokenService tokenService;
    private final DeviceService deviceService;
    private final SignInProcessor signInProcessor;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public LoginIdCheckResponseDto checkLoginId(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new CustomException(AccountResponseStatus.ACCOUNT_DUPLICATE_LOGIN_ID);
        }

        String token = loginIdTokenUtil.issue(loginId);
        return LoginIdCheckResponseDto.of(token);
    }

    @Override
    @Transactional
    public void signUp(SignUpRequestDto dto) {
        loginIdTokenUtil.validate(dto.getLoginId(), dto.getLoginIdToken());
        verificationService.validateConfirmed(dto.getEmail(), VerificationType.SIGN_UP);

        if (userRepository.existsByLoginId(dto.getLoginId())) {
            throw new CustomException(AccountResponseStatus.ACCOUNT_DUPLICATE_LOGIN_ID);
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL);
        }

        String password = passwordEncoder.encode(dto.getPassword());
        String tag = tagGenerator.generate(dto.getNickname());
        UserEntity user = userFactory.createFromSignUpRequest(dto, password, tag);
        userRepository.save(user);

        eventPublisher.publishEvent(UserSignedUpEvent.of(user));

        log.info("Sign Up Success - LoginId: {}", dto.getLoginId());
    }

    @Override
    @Transactional
    public SignInResult completeSignIn(CustomUserDetails userDetails, DeviceInfo deviceInfo, AuthToken previousTokens) {
        return signInProcessor.execute(userDetails, deviceInfo, previousTokens);
    }

    @Override
    @Transactional
    public SocialSignInResult processSocialLogin(OAuth2UserInfo userInfo, DeviceInfo deviceInfo) {
        return socialAccountRepository
                .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .map(account -> handleExistingSocialUser(account, userInfo, deviceInfo))
                .orElseGet(() -> handleNewSocialUser(userInfo, deviceInfo));
    }

    @Override
    @Transactional
    public SignInResult completeSocialSignUp(String tempToken, String email, DeviceInfo deviceInfo) {
        SocialSignUpClaims claims = socialSignUpTokenUtil.validate(tempToken);
        verificationService.validateConfirmed(email, VerificationType.SIGN_UP);

        if (userRepository.existsByEmail(email)) {
            throw new CustomException(SocialResponseStatus.SOCIAL_EMAIL_CONFLICT);
        }

        AuthProvider provider = AuthProvider.fromRegistrationId(claims.getProvider());

        if (socialAccountRepository.existsByProviderAndProviderId(provider, claims.getProviderId())) {
            throw new CustomException(SocialResponseStatus.SOCIAL_ALREADY_LINKED);
        }

        String password = passwordEncoder.encode(UUID.randomUUID().toString());
        String tag = tagGenerator.generate(claims.getNickname());
        UserEntity user = userFactory.createSocialUser(claims.getNickname(), email, password, provider, tag);
        userRepository.save(user);

        SocialAccountEntity socialAccount = SocialAccountEntity.builder()
                .userId(user.getId())
                .provider(provider)
                .providerId(claims.getProviderId())
                .providerEmail(email)
                .build();
        socialAccountRepository.save(socialAccount);

        eventPublisher.publishEvent(UserSignedUpEvent.of(user));

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getRole(),
                user.getEmail(),
                user.getNickname()
        );
        SignInResult signInResult = signInProcessor.execute(userDetails, deviceInfo, null);

        log.info("OAuth2 Sign-up Complete - UserPK: {} | Provider: {}", user.getId(), provider);

        return signInResult;
    }

    private SocialSignInResult handleExistingSocialUser(SocialAccountEntity existingAccount, OAuth2UserInfo userInfo, DeviceInfo deviceInfo) {
        existingAccount.updateProviderEmail(userInfo.getEmail());

        UserEntity user = userRepository.findById(existingAccount.getUserId())
                .orElseThrow(() -> new CustomException(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL));

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getRole(),
                user.getEmail(),
                user.getNickname()
        );
        SignInResult signInResult = signInProcessor.execute(userDetails, deviceInfo, null);

        log.info("OAuth2 Re-login - UserPK: {} | Provider: {}", user.getId(), userInfo.getProvider());

        return SocialSignInResult.Authenticated.of(signInResult);
    }

    private SocialSignInResult handleNewSocialUser(OAuth2UserInfo userInfo, DeviceInfo deviceInfo) {
        if (userInfo.getEmail() == null) {
            return SocialSignInResult.EmailRequired.of(
                    userInfo.getProvider().getValue(),
                    userInfo.getProviderId(),
                    userInfo.getNickname());
        }

        Optional<UserEntity> existing = userRepository.findByEmail(userInfo.getEmail());
        if (existing.isPresent()) {
            return SocialSignInResult.EmailConflict.of(existing.get().getAuthProvider().getValue());
        }

        String password = passwordEncoder.encode(UUID.randomUUID().toString());
        String tag = tagGenerator.generate(userInfo.getNickname());
        UserEntity user = userFactory.createSocialUser(userInfo.getNickname(), userInfo.getEmail(), password, userInfo.getProvider(), tag);
        userRepository.save(user);

        SocialAccountEntity socialAccount = SocialAccountEntity.builder()
                .userId(user.getId())
                .provider(userInfo.getProvider())
                .providerId(userInfo.getProviderId())
                .providerEmail(userInfo.getEmail())
                .build();
        socialAccountRepository.save(socialAccount);

        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getRole(),
                user.getEmail(),
                user.getNickname()
        );
        SignInResult signInResult = signInProcessor.execute(userDetails, deviceInfo, null);

        log.info("OAuth2 Sign-up - UserPK: {} | Provider: {}", user.getId(), userInfo.getProvider());

        return SocialSignInResult.Authenticated.of(signInResult);
    }

    @Override
    @Transactional
    public AuthToken reissue(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("Reissue Failed - Reason: Refresh Token is missing");
            throw new CustomAuthenticationException(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL);
        }

        CustomUserDetails userDetails = tokenService.authenticateRefreshToken(refreshToken);
        Long userId = userDetails.getId();
        String oldSessionId = userDetails.getSessionId();

        tokenService.validateRefreshToken(userId, oldSessionId, refreshToken);

        String newSessionId = deviceService.rotateDeviceSession(userId, oldSessionId);
        AuthToken newTokens = tokenService.rotateTokens(
                userId, userDetails.getRole(), oldSessionId, newSessionId);

        log.info("Token Reissue Success - UserPK: {} | SessionId: {} | Role: {}",
                userId, newSessionId, userDetails.getRole().name());

        return newTokens;
    }

}