package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.global.security.dto.AuthTokenDto;

public interface AuthService {

    // 회원가입
    void signUp(SignUpRequestDto dto);

    // 토큰 재발급
    AuthTokenDto reissue(AuthTokenDto oldTokens);

}