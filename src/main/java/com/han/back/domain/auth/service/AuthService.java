package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.token.AuthToken;

public interface AuthService {

    // 아이디 중복 체크
    LoginIdCheckResponseDto checkLoginId(String loginId);

    // 회원가입
    void signUp(SignUpRequestDto dto);

    // 로그인 완료 처리 — 디바이스 등록 + 토큰 발급
    SignInResult completeSignIn(CustomUserDetails userDetails, DeviceInfo deviceInfo, AuthToken previousTokens);

    // 소셜 로그인 처리 — 기존 계정 재로그인 / 신규 가입 / 이메일 필요 여부를 분기 반환
    SocialSignInResult processSocialLogin(OAuth2UserInfo userInfo, DeviceInfo deviceInfo);

    // 이메일을 제공하지 않는 소셜 가입 — 임시 토큰 + 이메일을 받아 회원가입 진행
    SignInResult completeSocialSignUp(String tempToken, String email, DeviceInfo deviceInfo);

    // 토큰 재발급
    AuthToken reissue(String refreshToken);

}