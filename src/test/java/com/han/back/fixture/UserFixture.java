package com.han.back.fixture;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class UserFixture {

    private UserFixture() {}

    public static final String DEFAULT_LOGIN_ID = "testuser";
    public static final String DEFAULT_NICKNAME = "테스트유저";
    public static final String RAW_PASSWORD = "Test1234!";

    public static UserEntity localUser() {
        return UserEntity.builder()
                .email("test@test.com")
                .nickname(DEFAULT_NICKNAME)
                .tag("A1B2")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    public static UserEntity adminUser() {
        return UserEntity.builder()
                .email("admin@test.com")
                .nickname("어드민유저")
                .tag("C3D4")
                .role(Role.ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    public static UserEntity socialUser() {
        return UserEntity.builder()
                .email("social@test.com")
                .nickname("소셜유저")
                .tag("E5F6")
                .role(Role.USER)
                .authProvider(AuthProvider.GOOGLE)
                .build();
    }

    public static CredentialEntity localCredential(Long userId, String loginId, PasswordEncoder encoder) {
        return CredentialEntity.builder()
                .userId(userId)
                .provider(AuthProvider.LOCAL)
                .identifier(loginId)
                .password(encoder.encode(RAW_PASSWORD))
                .build();
    }

}