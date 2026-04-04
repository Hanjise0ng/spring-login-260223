package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.mapper.UserMapper;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.LoginIdTokenUtil;
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
    public AuthTokenDto reissue(AuthTokenDto oldTokens) {
        if (!StringUtils.hasText(oldTokens.getRefreshToken())) {
            log.warn("Reissue Failed - Reason: Refresh Token is missing");
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        CustomUserDetails userDetails = tokenService.authenticateRefreshToken(oldTokens.getRefreshToken());
        Long userId = userDetails.getId();
        String oldSessionId = userDetails.getSessionId();

        tokenService.validateRefreshToken(
                userDetails.getId(), userDetails.getSessionId(), oldTokens.getRefreshToken()
        );

        String newSessionId = deviceService.rotateDeviceSession(userId, oldSessionId);
        AuthTokenDto newTokens = tokenService.rotateTokens(userId, userDetails.getRole(), oldSessionId, newSessionId);

        log.info("Token Reissue Success - UserPK: {} | SessionId: {} | Role: {}",
                userDetails.getId(), userDetails.getSessionId(), userDetails.getRole().name());

        return newTokens;
    }

}