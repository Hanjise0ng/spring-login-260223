package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.global.security.dto.AuthTokenDto;

public interface AuthService {

    LoginIdCheckResponseDto checkLoginId(String loginId);

    void signUp(SignUpRequestDto dto);

    AuthTokenDto reissue(AuthTokenDto oldTokens);

}