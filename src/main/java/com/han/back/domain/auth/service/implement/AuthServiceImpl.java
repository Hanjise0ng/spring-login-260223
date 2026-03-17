package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.mapper.UserMapper;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
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

    @Override
    @Transactional
    public void signUp(SignUpRequestDto dto) {
        String loginId = dto.getLoginId();
        if (userRepository.existsByLoginId(loginId)) {
            throw new CustomException(BaseResponseStatus.DUPLICATE_ID);
        }

        UserEntity user = userMapper.fromSignUpRequest(dto, passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
    }

    @Override
    public AuthTokenDto reissue(AuthTokenDto oldTokens) {
        if (!StringUtils.hasText(oldTokens.getRefreshToken())) {
            log.warn("Reissue Failed - Reason: Refresh Token is missing");
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        CustomUserDetails userDetails = tokenService.authenticateRefreshToken(oldTokens.getRefreshToken());
        tokenService.validateRefreshToken(userDetails.getId(), userDetails.getSessionId(), oldTokens.getRefreshToken());
        AuthTokenDto newTokens = tokenService.rotateTokens(userDetails.getId(), userDetails.getRole(), userDetails.getSessionId(), oldTokens);

        log.info("Token Reissue Success - UserPK: {} | SessionId: {} | Role: {}",
                userDetails.getId(), userDetails.getSessionId(), userDetails.getRole().name());

        return newTokens;
    }

}