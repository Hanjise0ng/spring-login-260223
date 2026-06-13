package com.han.back.integration;

import com.han.back.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("로그인 실패 rate limit 통합 테스트")
class LoginRateLimitTest extends IntegrationTestBase {

    private static final int ACCOUNT_LIMIT = 10;

    @Test
    @DisplayName("계정 실패 한도(10회) 도달 후 다음 시도에서 429를 반환한다")
    void accountFailureLimitExceeded() throws Exception {
        signUpAs("ratelimituser", "rate@test.com");

        for (int i = 0; i < ACCOUNT_LIMIT; i++) {
            signIn("ratelimituser", "WrongPass1!").andExpect(status().isUnauthorized());
        }

        signIn("ratelimituser", "WrongPass1!")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("로그인 성공 시 계정 실패 카운터가 초기화되어 다시 시도할 수 있다")
    void successResetsAccountCounter() throws Exception {
        signUpAs("resetuser", "reset@test.com");

        for (int i = 0; i < ACCOUNT_LIMIT - 1; i++) {
            signIn("resetuser", "WrongPass1!").andExpect(status().isUnauthorized());
        }

        signIn("resetuser", UserFixture.RAW_PASSWORD).andExpect(status().isOk());

        for (int i = 0; i < ACCOUNT_LIMIT - 1; i++) {
            signIn("resetuser", "WrongPass1!").andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("한도 도달 후에는 올바른 비밀번호로도 429로 차단된다")
    void limitExceededBlocksEvenCorrectPassword() throws Exception {
        signUpAs("blockeduser", "blocked@test.com");

        for (int i = 0; i < ACCOUNT_LIMIT; i++) {
            signIn("blockeduser", "WrongPass1!").andExpect(status().isUnauthorized());
        }

        signIn("blockeduser", UserFixture.RAW_PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    @DisplayName("차단(429) 상태에서 반복 시도해도 카운트가 늘지 않는다")
    void blockedRequestsDoNotIncrementCounter() throws Exception {
        signUpAs("blockeduser2", "blocked2@test.com");

        for (int i = 0; i < ACCOUNT_LIMIT; i++) {
            signIn("blockeduser2", "WrongPass1!").andExpect(status().isUnauthorized());
        }

        // 차단 상태에서 추가 시도 — 429. 카운트가 안 늘어야(필터가 막아 LoginFilter 미도달, recordFailure 안 됨)
        signIn("blockeduser2", "WrongPass1!").andExpect(status().isTooManyRequests());
        signIn("blockeduser2", UserFixture.RAW_PASSWORD).andExpect(status().isTooManyRequests());
    }

}