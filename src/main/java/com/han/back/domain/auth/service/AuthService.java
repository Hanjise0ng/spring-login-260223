package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.dto.response.ReissueResponseDto;
import com.han.back.global.security.token.AuthToken;

public interface AuthService {

    LoginIdCheckResponseDto checkLoginId(String loginId);

    void signUp(SignUpRequestDto dto);

    ReissueResponseDto reissue(AuthToken oldTokens);

}