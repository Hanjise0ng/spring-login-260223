package com.han.back.fixture;

import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

public final class UserFixture {

    private UserFixture() {}

    // 기본 로컬 사용자
    public static UserEntity localUser() {
        return UserEntity.builder()
                .loginId("testuser")
                .password("$2a$10$dummyEncodedPassword.ForTest")
                .email("test@test.com")
                .nickname("테스트유저")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }


    // admin 사용자
    public static UserEntity adminUser() {
        return UserEntity.builder()
                .loginId("adminuser")
                .password("$2a$10$dummyEncodedPassword.ForTest")
                .email("admin@test.com")
                .nickname("어드민유저")
                .role(Role.ADMIN)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }


    // 소셜 로그인 사용자
    public static UserEntity socialUser() {
        return UserEntity.builder()
                .loginId("google_123456")
                .password("SOCIAL_GOOGLE_" + UUID.randomUUID())
                .email("social@test.com")
                .nickname("소셜유저")
                .role(Role.USER)
                .authProvider(AuthProvider.GOOGLE)
                .build();
    }

    // 통합 테스트 전용
    public static UserEntity localUser(PasswordEncoder encoder) {
        return UserEntity.builder()
                .loginId("testuser")
                .password(encoder.encode("Test1234!"))
                .email("test@test.com")
                .nickname("테스트유저")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    public static final String DEFAULT_LOGIN_ID = "testuser";
    public static final String RAW_PASSWORD = "Test1234!";

}