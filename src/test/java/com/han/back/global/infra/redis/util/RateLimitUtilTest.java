package com.han.back.global.infra.redis.util;

import com.han.back.global.exception.CustomException;
import com.han.back.global.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitUtil")
class RateLimitUtilTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private RateLimitUtil rateLimitUtil;

    private static final String KEY = "rate:test:key";
    private static final Duration TTL = Duration.ofMinutes(10);

    @BeforeEach
    void setUp() {
        rateLimitUtil = new RateLimitUtil(redisTemplate);
    }

    @Nested
    @DisplayName("increment()")
    class Increment {

        @BeforeEach
        void setUpIncrement() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("첫 번째 호출(count = 1) → expire 설정 + 1 반환")
        void firstCall_setsExpireAndReturnsOne() {
            given(valueOps.increment(KEY)).willReturn(1L);

            long result = rateLimitUtil.increment(KEY, TTL);

            assertThat(result).isEqualTo(1L);
            then(redisTemplate).should(times(1)).expire(KEY, TTL);
        }

        @Test
        @DisplayName("두 번째 이후 호출(count > 1) → expire 재설정 없음 + count 반환")
        void subsequentCall_doesNotResetExpire() {
            given(valueOps.increment(KEY)).willReturn(3L);

            long result = rateLimitUtil.increment(KEY, TTL);

            assertThat(result).isEqualTo(3L);
            then(redisTemplate).should(never()).expire(any(), any(Duration.class));
        }

        @Test
        @DisplayName("increment 반환값 null → REDIS_ERROR")
        void nullFromRedis_throwsRedisError() {
            given(valueOps.increment(KEY)).willReturn(null);

            assertThatThrownBy(() -> rateLimitUtil.increment(KEY, TTL))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(ResponseStatus.REDIS_ERROR);
        }

        @Test
        @DisplayName("Redis 연결 오류 → REDIS_ERROR (원본 예외 래핑)")
        void redisConnectionError_throwsRedisError() {
            given(valueOps.increment(KEY)).willThrow(new RuntimeException("Connection refused"));

            assertThatThrownBy(() -> rateLimitUtil.increment(KEY, TTL))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(ResponseStatus.REDIS_ERROR);
        }

        @Test
        @DisplayName("CustomException은 그대로 전파 (이중 래핑 방지)")
        void customException_propagatesUnwrapped() {
            CustomException original = new CustomException(ResponseStatus.REDIS_ERROR);
            given(valueOps.increment(KEY)).willThrow(original);

            assertThatThrownBy(() -> rateLimitUtil.increment(KEY, TTL))
                    .isSameAs(original);
        }

        @Test
        @DisplayName("첫 번째 호출 → expire에 정확한 TTL이 전달된다")
        void firstCall_expireCalledWithExactTtl() {
            given(valueOps.increment(KEY)).willReturn(1L);

            rateLimitUtil.increment(KEY, TTL);

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            then(redisTemplate).should().expire(eq(KEY), ttlCaptor.capture());
            assertThat(ttlCaptor.getValue()).isEqualTo(TTL);
        }
    }

    @Nested
    @DisplayName("incrementHourly()")
    class IncrementHourly {

        private static final String KEY_PREFIX = "rate:hourly:send:SIGN_UP:test@example.com";

        @BeforeEach
        void setUpIncrementHourly() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("생성되는 key에 keyPrefix가 포함된다")
        void key_containsKeyPrefix() {
            given(valueOps.increment(anyString())).willReturn(1L);

            rateLimitUtil.incrementHourly(KEY_PREFIX);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(valueOps).should().increment(keyCaptor.capture());
            assertThat(keyCaptor.getValue()).startsWith(KEY_PREFIX + ":");
        }

        @Test
        @DisplayName("생성되는 key의 window 값은 현재 정시 epoch와 일치한다")
        void key_windowAlignedToHour() {
            given(valueOps.increment(anyString())).willReturn(1L);

            long before = Instant.now().getEpochSecond();
            rateLimitUtil.incrementHourly(KEY_PREFIX);
            long after = Instant.now().getEpochSecond();

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(valueOps).should().increment(keyCaptor.capture());

            // key 마지막 ':' 이후가 window epoch 값
            String capturedKey = keyCaptor.getValue();
            long windowValue = Long.parseLong(capturedKey.substring(capturedKey.lastIndexOf(':') + 1));

            long windowSeconds = Duration.ofHours(1).toSeconds();
            long expectedWindow = (before / windowSeconds) * windowSeconds;
            long expectedWindowEnd = (after / windowSeconds) * windowSeconds;

            // before와 after가 같은 window에 속한다면 동일해야 함
            assertThat(windowValue).isBetween(expectedWindow, expectedWindowEnd);
            assertThat(windowValue % windowSeconds).isEqualTo(0); // 정시 정렬 확인
        }

        @Test
        @DisplayName("TTL은 1시간으로 설정된다 (count==1일 때)")
        void firstCall_ttlIsOneHour() {
            given(valueOps.increment(anyString())).willReturn(1L);

            rateLimitUtil.incrementHourly(KEY_PREFIX);

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            then(redisTemplate).should().expire(anyString(), ttlCaptor.capture());
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(1));
        }

        @Test
        @DisplayName("두 번의 연속 호출 → 같은 window에서 동일한 key 사용")
        void twoCallsInSameWindow_useSameKey() {
            given(valueOps.increment(anyString())).willReturn(1L, 2L);

            rateLimitUtil.incrementHourly(KEY_PREFIX);
            rateLimitUtil.incrementHourly(KEY_PREFIX);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(valueOps).should(times(2)).increment(keyCaptor.capture());

            assertThat(keyCaptor.getAllValues().get(0))
                    .isEqualTo(keyCaptor.getAllValues().get(1));
        }

        @Test
        @DisplayName("Redis 오류 → REDIS_ERROR 전파")
        void redisError_propagates() {
            given(valueOps.increment(anyString())).willThrow(new RuntimeException("timeout"));

            assertThatThrownBy(() -> rateLimitUtil.incrementHourly(KEY_PREFIX))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(ResponseStatus.REDIS_ERROR);
        }
    }

    @Nested
    @DisplayName("reset()")
    class Reset {

        @Test
        @DisplayName("정상 호출 → redisTemplate.delete(key) 1회 실행")
        void normalCall_deletesKey() {
            given(redisTemplate.delete(KEY)).willReturn(Boolean.TRUE);

            rateLimitUtil.reset(KEY);

            then(redisTemplate).should(times(1)).delete(KEY);
        }

        @Test
        @DisplayName("Redis 오류 → REDIS_ERROR")
        void redisError_throwsRedisError() {
            given(redisTemplate.delete(KEY)).willThrow(new RuntimeException("Connection reset"));

            assertThatThrownBy(() -> rateLimitUtil.reset(KEY))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(ResponseStatus.REDIS_ERROR);
        }

        @Test
        @DisplayName("존재하지 않는 키로 reset → 예외 없이 종료 (멱등 계약)")
        void nonExistentKey_isIdempotent() {
            given(redisTemplate.delete(anyString())).willReturn(Boolean.FALSE);

            assertThatCode(() -> rateLimitUtil.reset("non:existent:key"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("reset 시 정확한 키가 delete에 전달된다")
        void reset_passesExactKeyToDelete() {
            given(redisTemplate.delete(anyString())).willReturn(Boolean.TRUE);

            String specificKey = "rate:verify:fail:SIGN_UP:user@test.com";

            rateLimitUtil.reset(specificKey);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(redisTemplate).should().delete(keyCaptor.capture());
            assertThat(keyCaptor.getValue()).isEqualTo(specificKey);
        }
    }

    @Nested
    @DisplayName("currentWindow() — incrementHourly 경유 간접 검증")
    class CurrentWindow {

        @BeforeEach
        void setUpCurrentWindow() {
            given(redisTemplate.opsForValue()).willReturn(valueOps);
        }

        @Test
        @DisplayName("window 경계값: epoch 를 3600으로 나눈 나머지가 0 (정시 정렬)")
        void windowIsAlignedToHourBoundary() {
            given(valueOps.increment(anyString())).willReturn(1L);

            rateLimitUtil.incrementHourly("prefix");

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(valueOps).should().increment(keyCaptor.capture());

            String key = keyCaptor.getValue();
            String windowStr = key.substring(key.lastIndexOf(':') + 1);
            long windowEpoch = Long.parseLong(windowStr);

            assertThat(windowEpoch % 3600).isEqualTo(0);
        }

        @Test
        @DisplayName("window 값은 과거 시각 (현재 epoch 이하)")
        void windowIsNotInFuture() {
            given(valueOps.increment(anyString())).willReturn(1L);

            long now = Instant.now().getEpochSecond();
            rateLimitUtil.incrementHourly("prefix");

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(valueOps).should().increment(keyCaptor.capture());

            String key = keyCaptor.getValue();
            long windowEpoch = Long.parseLong(key.substring(key.lastIndexOf(':') + 1));

            assertThat(windowEpoch).isLessThanOrEqualTo(now);
        }
    }

}