package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.mapper.UserMapper;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
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
    private final JwtUtil jwtUtil;
    private final TokenService tokenService;

    @Override
    @Transactional
    public void signUp(SignUpRequestDto dto) {
        String userId = dto.getUserId();
        if (userRepository.existsByUserId(userId)) {
            throw new CustomException(BaseResponseStatus.DUPLICATE_ID);
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        UserEntity user = userMapper.ofSignupDTO(dto);

        userRepository.save(user);
    }

    @Override
    public AuthTokenDto reissue(AuthTokenDto oldTokens) {
        // Refresh Token 존재 여부 확인
        if (!StringUtils.hasText(oldTokens.getRefreshToken())) {
            log.warn("Reissue Failed - Reason: Refresh Token is missing");
            throw new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        // 토큰 파싱 및 유효성(만료/서명) 검증
        Claims claims = jwtUtil.validateAndGetPayload(oldTokens.getRefreshToken());

        // 토큰 카테고리 검증
        String category = jwtUtil.getCategory(claims);
        if (!AuthConst.TOKEN_TYPE_REFRESH.equals(category)) {
            log.warn("Reissue Failed - Reason: Not a Refresh Token");
            throw new CustomAuthenticationException(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        Long id = jwtUtil.getUserId(claims);
        Role role = jwtUtil.getRole(claims);

        // Redis 대조 검증 (탈취 방어)
        tokenService.verifyRefreshTokenOwnership(id, oldTokens.getRefreshToken());

        // 구형 토큰 무효화 및 신규 발급/저장
        AuthTokenDto newTokens = tokenService.rotateTokens(id, role, oldTokens);

        log.info("Token Reissue Success - UserId: {} | Role: {}", id, role.name());
        return newTokens;
    }

}