package com.han.back.global.infra.notification;

import com.han.back.global.infra.redis.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationIdempotencyGuard")
class NotificationIdempotencyGuardTest {

    @Mock private RedisUtil redisUtil;

    @InjectMocks private NotificationIdempotencyGuard guard;

    @Test
    @DisplayName("최초 요청: SET NX 성공 → true 반환")
    void firstRequest_returnsTrue() {
        given(redisUtil.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(true);

        boolean result = guard.tryAcquire("welcome:user:42", Duration.ofDays(1));

        assertThat(result).isTrue();
        then(redisUtil).should(times(1))
                .setIfAbsent(eq("notification:dedupe:welcome:user:42"), eq("1"), eq(Duration.ofDays(1)));
    }

    @Test
    @DisplayName("중복 요청: SET NX 실패 → false 반환")
    void duplicateRequest_returnsFalse() {
        given(redisUtil.setIfAbsent(anyString(), anyString(), any(Duration.class))).willReturn(false);

        boolean result = guard.tryAcquire("welcome:user:42", Duration.ofDays(1));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("null dedupeKey: 멱등성 검사 생략 → true 반환")
    void nullKey_bypasses() {
        boolean result = guard.tryAcquire(null, Duration.ofHours(1));

        assertThat(result).isTrue();
        then(redisUtil).should(never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("blank dedupeKey: 멱등성 검사 생략 → true 반환")
    void blankKey_bypasses() {
        boolean result = guard.tryAcquire("", Duration.ofHours(1));

        assertThat(result).isTrue();
        then(redisUtil).should(never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

}