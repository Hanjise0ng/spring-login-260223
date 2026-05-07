package com.han.back.global.security.logout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LogoutContext")
class LogoutContextTest {

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
    }

    @Nested
    @DisplayName("setResult()")
    class SetResult {

        @ParameterizedTest(name = "{0} → request attribute에 저장된다")
        @EnumSource(LogoutResult.class)
        @DisplayName("모든 LogoutResult 값이 attribute에 저장된다")
        void allResults_storedInAttribute(LogoutResult result) {
            LogoutContext.setResult(request, result);

            Object stored = request.getAttribute(LogoutContext.KEY);
            assertThat(stored).isEqualTo(result);
        }
    }

    @Nested
    @DisplayName("getResult()")
    class GetResult {

        @ParameterizedTest(name = "{0} → 저장된 값 그대로 반환된다")
        @EnumSource(LogoutResult.class)
        @DisplayName("저장된 LogoutResult 값이 그대로 반환된다")
        void storedResult_returnedAsIs(LogoutResult result) {
            LogoutContext.setResult(request, result);

            assertThat(LogoutContext.getResult(request)).isEqualTo(result);
        }

        @Test
        @DisplayName("attribute가 null이면 UNAUTHENTICATED를 반환한다")
        void nullAttribute_returnsUnauthenticated() {
            // attribute를 설정하지 않은 상태 = 미인증 요청
            assertThat(LogoutContext.getResult(request))
                    .isEqualTo(LogoutResult.UNAUTHENTICATED);
        }

        @Test
        @DisplayName("attribute가 LogoutResult가 아닌 타입이면 UNAUTHENTICATED를 반환한다")
        void wrongType_returnsUnauthenticated() {
            request.setAttribute(LogoutContext.KEY, "invalid-string-type");

            assertThat(LogoutContext.getResult(request))
                    .isEqualTo(LogoutResult.UNAUTHENTICATED);
        }
    }

    @Nested
    @DisplayName("setResult() → getResult() 라운드트립")
    class RoundTrip {

        @Test
        @DisplayName("set한 값을 get으로 그대로 꺼낼 수 있다 — 핵심 계약")
        void setThenGet_returnsStoredValue() {

            LogoutContext.setResult(request, LogoutResult.SUCCESS);

            assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.SUCCESS);
        }

        @Test
        @DisplayName("마지막 set 값이 get에 반영된다 — 덮어쓰기 계약")
        void lastSetWins() {
            LogoutContext.setResult(request, LogoutResult.REDIS_ERROR);
            LogoutContext.setResult(request, LogoutResult.SUCCESS);

            assertThat(LogoutContext.getResult(request)).isEqualTo(LogoutResult.SUCCESS);
        }
    }

}