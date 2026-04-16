package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.mapper.UserMapper;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.LoginIdTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final DeviceService deviceService;
    private final VerificationService verificationService;
    private final LoginIdTokenUtil loginIdTokenUtil;

    @Override
    @Transactional(readOnly = true)
    public LoginIdCheckResponseDto checkLoginId(String loginId) {
        if (userRepository.existsByLoginId(loginId)) {
            throw new CustomException(BaseResponseStatus.DUPLICATE_ID);
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
            throw new CustomException(BaseResponseStatus.DUPLICATE_ID);
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(BaseResponseStatus.DUPLICATE_EMAIL);
        }

        UserEntity user = userMapper.fromSignUpRequest(dto, passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);

        verificationService.consumeConfirmation(dto.getEmail(), VerificationType.SIGN_UP);

        log.info("Sign Up Success - LoginId: {}", dto.getLoginId());
    }

    @Override
    @Transactional
    public SignInResult completeSignIn(CustomUserDetails userDetails,
                                       DeviceInfo deviceInfo,
                                       AuthToken previousTokens) {
        Long userId = userDetails.getId();
        invalidatePreviousSessionIfPresent(userId, previousTokens);

        DeviceRegistration registration = deviceService.registerLoginDevice(userId, deviceInfo);

        AuthToken tokens = tokenService.issueTokens(
                userId, userDetails.getRole(), registration.getSessionId());

        log.info("Login Success - UserPK: {} | Role: {} | SessionId: {} | DeviceType: {}",
                userId, userDetails.getRole().name(),
                registration.getSessionId(), deviceInfo.getDeviceType().name());

        return SignInResult.of(tokens, registration.getDeviceFingerprint());
    }

    @Override
    @Transactional
    public AuthToken reissue(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("Reissue Failed - Reason: Refresh Token is missing");
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
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

    private void invalidatePreviousSessionIfPresent(Long userId, AuthToken previousTokens) {
        if (previousTokens == null || previousTokens.isEmpty()) return;

        tokenService.extractUserFromTokens(
                        previousTokens.getAccessToken(),
                        previousTokens.getRefreshToken())
                .filter(prev -> prev.getSessionId() != null)
                .ifPresent(prev -> {
                    tokenService.invalidateSession(userId, prev.getSessionId());
                    log.debug("Previous session invalidated on re-login - UserPK: {} | OldSessionId: {}",
                            userId, prev.getSessionId());
                });
    }

}