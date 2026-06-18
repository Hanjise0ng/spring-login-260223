package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.auth.factory.UserFactory;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final TokenService tokenService;
    private final DeviceService deviceService;
    private final VerificationService verificationService;
    private final CredentialLinkService credentialLinkService;
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final UserFactory userFactory;
    private final LoginIdTokenUtil loginIdTokenUtil;
    private final SocialSignUpTokenUtil socialSignUpTokenUtil;
    private final TagGenerator tagGenerator;
    private final PasswordEncoder passwordEncoder;
    private final SignInProcessor signInProcessor;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public LoginIdCheckResponseDto checkLoginId(String loginId) {
        if (credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, loginId)) {
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

        if (credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, dto.getLoginId())) {
            throw new CustomException(AccountResponseStatus.ACCOUNT_DUPLICATE_LOGIN_ID);
        }
        if (existsLocalEmail(dto.getEmail())) {
            throw new CustomException(AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL);
        }

        String tag = tagGenerator.generate(dto.getNickname());
        UserEntity user = userFactory.createLocalUser(dto, tag);
        userRepository.save(user);

        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        CredentialEntity credential = userFactory.createLocalCredential(user.getId(), dto.getLoginId(), encodedPassword);
        credentialRepository.save(credential);

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
        return credentialRepository
                .findByProviderAndIdentifier(userInfo.getProvider(), userInfo.getProviderId())
                .map(credential -> handleExistingSocialUser(credential, userInfo, deviceInfo))
                .orElseGet(() -> handleNewSocialUser(userInfo, deviceInfo));
    }

    @Override
    @Transactional
    public SocialSignInResult completeSocialSignUp(String tempToken, String email, DeviceInfo deviceInfo) {
        SocialSignUpClaims claims = socialSignUpTokenUtil.validate(tempToken);
        verificationService.validateConfirmed(email, VerificationType.SIGN_UP);

        if (existsLocalEmail(email)) {
            return SocialSignInResult.LinkSuggested.of(
                    claims.getProvider(), claims.getProviderId(), claims.getNickname());
        }

        AuthProvider provider = AuthProvider.fromRegistrationId(claims.getProvider());
        SignInResult signInResult = createSocialAccountAndSignIn(
                provider, claims.getProviderId(), claims.getNickname(), email, deviceInfo);

        log.info("OAuth2 Sign-up Complete - Provider: {}", provider);

        return SocialSignInResult.Authenticated.of(signInResult);
    }

    @Override
    @Transactional
    public SocialSignInResult createSeparateSocialAccount(String tempToken, String email, DeviceInfo deviceInfo) {
        SocialSignUpClaims claims = socialSignUpTokenUtil.validate(tempToken);
        verificationService.validateConfirmed(email, VerificationType.SIGN_UP);

        AuthProvider provider = AuthProvider.fromRegistrationId(claims.getProvider());
        SignInResult signInResult = createSocialAccountAndSignIn(
                provider, claims.getProviderId(), claims.getNickname(), email, deviceInfo);

        log.info("Separate Social Account Created - Provider: {}", provider);

        return SocialSignInResult.Authenticated.of(signInResult);
    }

    @Override
    @Transactional
    public void linkSocialToLocalAccount(String tempToken, String loginId, String password) {
        SocialSignUpClaims claims = socialSignUpTokenUtil.validate(tempToken);

        Long userId = authenticateLocalCredential(loginId, password);

        AuthProvider provider = AuthProvider.fromRegistrationId(claims.getProvider());
        credentialLinkService.linkSocialCredential(userId, provider, claims.getProviderId());

        log.info("Social Linked to Local Account - UserPK: {} | Provider: {}", userId, provider);
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

    private SocialSignInResult handleExistingSocialUser(CredentialEntity existingCredential, OAuth2UserInfo userInfo, DeviceInfo deviceInfo) {
        UserEntity user = userRepository.findById(existingCredential.getUserId())
                .orElseThrow(() -> new CustomException(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL));

        CustomUserDetails userDetails = CustomUserDetails.forSocialLogin(
                user.getId(), user.getRole(), user.getEmail(), user.getNickname());
        SignInResult signInResult = signInProcessor.execute(userDetails, deviceInfo, null);

        log.info("OAuth2 Re-login - UserPK: {} | Provider: {}", user.getId(), userInfo.getProvider());

        return SocialSignInResult.Authenticated.of(signInResult);
    }

    private SocialSignInResult handleNewSocialUser(OAuth2UserInfo userInfo, DeviceInfo deviceInfo) {
        if (userInfo.getEmail() == null) {
            return SocialSignInResult.EmailRequired.of(
                    userInfo.getProvider().getValue(), userInfo.getProviderId(), userInfo.getNickname());
        }

        if (existsLocalEmail(userInfo.getEmail())) {
            return SocialSignInResult.LinkSuggested.of(
                    userInfo.getProvider().getValue(), userInfo.getProviderId(), userInfo.getNickname());
        }

        SignInResult signInResult = createSocialAccountAndSignIn(
                userInfo.getProvider(), userInfo.getProviderId(), userInfo.getNickname(),
                userInfo.getEmail(), deviceInfo);

        log.info("OAuth2 Sign-up - Provider: {}", userInfo.getProvider());

        return SocialSignInResult.Authenticated.of(signInResult);
    }

    private SignInResult createSocialAccountAndSignIn(AuthProvider provider, String providerId, String nickname, String email, DeviceInfo deviceInfo) {
        if (credentialRepository.existsByProviderAndIdentifier(provider, providerId)) {
            throw new CustomException(SocialResponseStatus.SOCIAL_ALREADY_LINKED);
        }

        String tag = tagGenerator.generate(nickname);
        UserEntity user = userFactory.createSocialUser(nickname, email, tag);
        userRepository.save(user);

        CredentialEntity credential = userFactory.createSocialCredential(user.getId(), provider, providerId);
        credentialRepository.save(credential);

        eventPublisher.publishEvent(UserSignedUpEvent.of(user));

        CustomUserDetails userDetails = CustomUserDetails.forSocialLogin(
                user.getId(), user.getRole(), user.getEmail(), user.getNickname());

        return signInProcessor.execute(userDetails, deviceInfo, null);
    }

    private boolean existsLocalEmail(String email) {
        return userRepository.findByEmail(email)
                .map(UserEntity::getId)
                .map(userId -> credentialRepository.findByUserIdAndProvider(userId, AuthProvider.LOCAL).isPresent())
                .orElse(false);
    }

    private Long authenticateLocalCredential(String loginId, String password) {
        CredentialEntity localCredential = credentialRepository
                .findByProviderAndIdentifier(AuthProvider.LOCAL, loginId)
                .orElseThrow(() -> new CustomAuthenticationException(AuthResponseStatus.AUTH_SIGN_IN_FAIL));

        if (!passwordEncoder.matches(password, localCredential.getPassword())) {
            throw new CustomAuthenticationException(AuthResponseStatus.AUTH_SIGN_IN_FAIL);
        }

        return localCredential.getUserId();
    }

}