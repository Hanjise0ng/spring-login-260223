package com.han.back.domain.verification.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VerificationType {

    SIGN_UP("회원가입 인증", "[HAN] 회원가입 인증 코드"),
    PASSWORD_RESET("비밀번호 재설정 인증", "[HAN] 비밀번호 재설정 인증 코드"),
    STEP_UP("본인 확인 인증", "[HAN] 본인 확인 인증 코드"),
    EMAIL_CHANGE("이메일 변경 인증", "[HAN] 이메일 변경 인증 코드");

    private final String description;
    private final String emailSubject;

}