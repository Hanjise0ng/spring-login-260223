package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.token.AuthToken;

public interface AuthService {

    LoginIdCheckResponseDto checkLoginId(String loginId);

    void signUp(SignUpRequestDto dto);

    SignInResult completeSignIn(CustomUserDetails userDetails, DeviceInfo deviceInfo, AuthToken previousTokens);

    AuthToken reissue(String refreshToken);

}