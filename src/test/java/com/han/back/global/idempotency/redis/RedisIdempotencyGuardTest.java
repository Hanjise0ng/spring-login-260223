package com.han.back.global.idempotency.redis;

import com.han.back.global.idempotency.IdempotencyResult;
import com.han.back.global.infra.redis.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisIdempotencyGuard")
class RedisIdempotencyGuardTest {

    @Mock private RedisUtil redisUtil;

    private RedisIdempotencyGuard guard;

    private static final String SCOPE    = "notification";
    private static final String KEY      = "welcome:user:100";
    private static final String REDIS_KEY = "idempotency:" + SCOPE + ":" + KEY;
    private static final Duration TTL    = Duration.ofDays(1);

    @BeforeEach
    void setUp() {
        guard = new RedisIdempotencyGuard(redisUtil);
    }

    @Nested
    @DisplayName("tryAcquire() — 키 유효성")
    class KeyValidation {

        @Test
        @DisplayName("null 키 → Redis 호출 없이 ACQUIRED 반환 (멱등성 우회)")
        void nullKey_returnsAcquiredWithoutRedis() {
            IdempotencyResult result = guard.tryAcquire(SCOPE, null, TTL);

            assertThat(result).isEqualTo(IdempotencyResult.ACQUIRED);
            then(redisUtil).should(never()).setIfAbsent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("blank 키 → Redis 호출 없이 ACQUIRED 반환")
        void blankKey_returnsAcquiredWithoutRedis() {
            IdempotencyResult result = guard.tryAcquire(SCOPE, "   ", TTL);

            assertThat(result).isEqualTo(IdempotencyResult.ACQUIRED);
            then(redisUtil).should(never()).setIfAbsent(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("empty 키 → Redis 호출 없이 ACQUIRED 반환")
        void emptyKey_returnsAcquiredWithoutRedis() {
            IdempotencyResult result = guard.tryAcquire(SCOPE, "", TTL);

            assertThat(result).isEqualTo(IdempotencyResult.ACQUIRED);
            then(redisUtil).should(never()).setIfAbsent(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("tryAcquire() — 획득 경로")
    class AcquirePath {

        @Test
        @DisplayName("최초 요청: SETNX 성공 → ACQUIRED 반환")
        void firstRequest_setnxSucceeds_returnsAcquired() {
            given(redisUtil.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), eq(TTL)))
                    .willReturn(true);

            IdempotencyResult result = guard.tryAcquire(SCOPE, KEY, TTL);

            assertThat(result).isEqualTo(IdempotencyResult.ACQUIRED);
            // Redis 키 포맷 계약: "idempotency:{scope}:{key}" + 값은 "PROCESSING"
            then(redisUtil).should(times(1))
                    .setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), eq(TTL));
        }

        @Test
        @DisplayName("race condition — TTL 만료 직후 재시도 SETNX 성공 → ACQUIRED 반환")
        void racecondition_ttlExpired_retrySucceeds_returnsAcquired() {
            given(redisUtil.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), eq(TTL)))
                    .willReturn(false)   // 1차: 다른 프로세스가 선점
                    .willReturn(true);   // 2차: TTL 만료 후 재시도 성공
            given(redisUtil.getData(REDIS_KEY)).willReturn(Optional.empty()); // TTL 만료

            IdempotencyResult result = guard.tryAcquire(SCOPE, KEY, TTL);

            assertThat(result).isEqualTo(IdempotencyResult.ACQUIRED);
        }
    }

    @Nested
    @DisplayName("tryAcquire() — 중복/처리 중 경로")
    class DuplicatePath {

        @Test
        @DisplayName("이미 COMPLETED 상태 → COMPLETED 반환 (중복 요청 차단)")
        void alreadyCompleted_returnsCompleted() {
            given(redisUtil.setIfAbsent(eq(REDIS_KEY), anyString(), eq(TTL))).willReturn(false);
            given(redisUtil.getData(REDIS_KEY)).willReturn(Optional.of("COMPLETED"));

            IdempotencyResult result = guard.tryAcquire(SCOPE, KEY, TTL);

            assertThat(result).isEqualTo(IdempotencyResult.COMPLETED);
        }

        @Test
        @DisplayName("PROCESSING 상태 → PROCESSING 반환 (다른 프로세스 처리 중)")
        void currentlyProcessing_returnsProcessing() {
            given(redisUtil.setIfAbsent(eq(REDIS_KEY), anyString(), eq(TTL))).willReturn(false);
            given(redisUtil.getData(REDIS_KEY)).willReturn(Optional.of("PROCESSING"));

            IdempotencyResult result = guard.tryAcquire(SCOPE, KEY, TTL);

            assertThat(result).isEqualTo(IdempotencyResult.PROCESSING);
        }

        @Test
        @DisplayName("race condition — 재시도 SETNX도 실패 → PROCESSING 반환")
        void raceCondition_retryAlsoFails_returnsProcessing() {
            // 시나리오: 1차 SETNX 실패 → TTL 만료 → 2차 SETNX도 실패
            given(redisUtil.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), eq(TTL)))
                    .willReturn(false)
                    .willReturn(false); // 재시도도 실패
            given(redisUtil.getData(REDIS_KEY)).willReturn(Optional.empty());

            IdempotencyResult result = guard.tryAcquire(SCOPE, KEY, TTL);

            assertThat(result).isEqualTo(IdempotencyResult.PROCESSING);
        }
    }

    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("정상 키 → COMPLETED 상태로 갱신, completeTtl로 만료 설정")
        void validKey_setsCompletedWithTtl() {
            Duration completeTtl = Duration.ofMinutes(30);

            guard.complete(SCOPE, KEY, completeTtl);

            // 값이 "COMPLETED"로, TTL이 completeTtl로 설정되는 계약
            then(redisUtil).should(times(1))
                    .setDataExpire(eq(REDIS_KEY), eq("COMPLETED"), eq(completeTtl));
        }

        @Test
        @DisplayName("null 키 → Redis 호출 없이 종료 (멱등)")
        void nullKey_noRedisCall() {
            guard.complete(SCOPE, null, TTL);

            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("blank 키 → Redis 호출 없이 종료")
        void blankKey_noRedisCall() {
            guard.complete(SCOPE, "", TTL);

            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), any());
        }
    }

    @Nested
    @DisplayName("release()")
    class Release {

        @Test
        @DisplayName("정상 키 → Redis 키 삭제 (재시도 가능 상태로 복구)")
        void validKey_deletesRedisKey() {
            guard.release(SCOPE, KEY);

            then(redisUtil).should(times(1)).deleteData(eq(REDIS_KEY));
        }

        @Test
        @DisplayName("null 키 → Redis 호출 없이 종료 (멱등)")
        void nullKey_noRedisCall() {
            guard.release(SCOPE, null);

            then(redisUtil).should(never()).deleteData(anyString());
        }

        @Test
        @DisplayName("blank 키 → Redis 호출 없이 종료")
        void blankKey_noRedisCall() {
            guard.release(SCOPE, "");

            then(redisUtil).should(never()).deleteData(anyString());
        }

        @Test
        @DisplayName("release 후 동일 키로 tryAcquire 가능 — 재시도 가능 상태 계약")
        void afterRelease_sameKeyCanBeAcquiredAgain() {
            guard.release(SCOPE, KEY);

            given(redisUtil.setIfAbsent(eq(REDIS_KEY), eq("PROCESSING"), eq(TTL)))
                    .willReturn(true);

            IdempotencyResult result = guard.tryAcquire(SCOPE, KEY, TTL);

            assertThat(result).isEqualTo(IdempotencyResult.ACQUIRED);
        }
    }

}