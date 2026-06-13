package com.han.back.integration;

import com.han.back.domain.user.entity.UserEntity;
import com.han.back.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("탈퇴 유예 계정 로그인 통합 테스트")
class SignInDeletedAccountTest extends IntegrationTestBase {

    @Test
    @DisplayName("탈퇴 유예 계정으로 올바른 비번 로그인 시 복구 신호와 남은 유예일수를 받는다")
    void deletedAccountLoginReturnsRecoveryGuidance() throws Exception {
        UserEntity user = signUpAs("deleteduser", "deleted@test.com");
        user.softDelete(LocalDateTime.now().minusDays(7));
        userRepository.save(user);

        // 날짜 경계 계산(ChronoUnit.DAYS.between)이라 7일 전 탈퇴 → 30 - 7 = 23일로 결정적.
        // toDays() 내림과 달리 시각에 무관하므로 정확한 값으로 단언한다.
        signIn("deleteduser", UserFixture.RAW_PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_DELETED"))
                .andExpect(jsonPath("$.result.gracePeriodRemainingDays").value(23));
    }

    @Test
    @DisplayName("탈퇴 직후(오늘) 로그인 시 남은 유예일수는 30일이다")
    void deletedToday_remainingDaysIsFull() throws Exception {
        UserEntity user = signUpAs("deleteduser2", "deleted2@test.com");
        user.softDelete(LocalDateTime.now());
        userRepository.save(user);

        signIn("deleteduser2", UserFixture.RAW_PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACCOUNT_ALREADY_DELETED"))
                .andExpect(jsonPath("$.result.gracePeriodRemainingDays").value(30));
    }

    @Test
    @DisplayName("탈퇴 계정에 틀린 비번이면 탈퇴 상태를 노출하지 않고 일반 실패한다")
    void deletedAccountWrongPasswordDoesNotLeakState() throws Exception {
        UserEntity user = signUpAs("deleteduser3", "deleted3@test.com");
        user.softDelete(LocalDateTime.now());
        userRepository.save(user);

        signIn("deleteduser3", "WrongPass1!")
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("탈퇴 유예 계정 로그인은 정상 토큰을 발급하지 않는다")
    void deletedAccountLoginIssuesNoTokens() throws Exception {
        UserEntity user = signUpAs("deleteduser4", "deleted4@test.com");
        user.softDelete(LocalDateTime.now());
        userRepository.save(user);

        signIn("deleteduser4", UserFixture.RAW_PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.result.tokens").doesNotExist());
    }

}