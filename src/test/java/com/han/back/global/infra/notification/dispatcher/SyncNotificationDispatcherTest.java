package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.idempotency.IdempotencyGuard;
import com.han.back.global.idempotency.IdempotencyResult;
import com.han.back.global.infra.notification.model.*;
import com.han.back.global.infra.notification.sender.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncNotificationDispatcherTest {

    @Mock private NotificationSender emailSender;
    @Mock private IdempotencyGuard idempotencyGuard;

    private SyncNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
        dispatcher = new SyncNotificationDispatcher(List.of(emailSender), idempotencyGuard);
    }

    private static NotificationCommand buildCommand(
            NotificationPurpose purpose,
            NotificationChannel channel,
            String dedupeKey
    ) {
        NotificationRequest request = NotificationRequest.of(
                channel, "test@example.com", "제목", "본문", purpose
        );
        NotificationMetadata metadata = NotificationMetadata.of("trace-001", dedupeKey);
        return NotificationCommand.of(request, metadata);
    }

    static Stream<Arguments> purposeTtlProvider() {
        return Stream.of(NotificationPurpose.values())
                .map(p -> Arguments.of(p, p.getDedupeTtl()));
    }

    @Test
    @DisplayName("중복 요청(COMPLETED/PROCESSING)이면 발송을 생략한다")
    void skipDuplicateDispatch() {
        // given
        NotificationCommand command = buildCommand(
                NotificationPurpose.VERIFICATION, NotificationChannel.EMAIL, "dedupe-123"
        );
        // tryAcquire 3인자 / IdempotencyResult 반환
        given(idempotencyGuard.tryAcquire(anyString(), anyString(), any(Duration.class)))
                .willReturn(IdempotencyResult.COMPLETED);

        // when
        dispatcher.dispatch(command);

        // then
        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("등록되지 않은 채널이면 release() 후 IllegalStateException을 던진다")
    void noSenderFoundThrowsException() {
        // given - SMS 채널로 요청하지만 emailSender만 등록된 상황
        NotificationCommand command = buildCommand(
                NotificationPurpose.WELCOME, NotificationChannel.SMS, "dedupe-456"
        );
        given(idempotencyGuard.tryAcquire(anyString(), anyString(), any(Duration.class)))
                .willReturn(IdempotencyResult.ACQUIRED);

        // when & then
        assertThatThrownBy(() -> dispatcher.dispatch(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No sender for channel");

        // Sync에서는 sender 없을 때 acquire한 key를 반드시 release 후 예외를 던져야 함
        // release()가 없으면 해당 dedupeKey가 PROCESSING 상태로 영구 잠금됨
        verify(idempotencyGuard).release(anyString(), eq("dedupe-456"));
    }

    @Test
    @DisplayName("발송 중 예외가 발생하면 release() 후 예외를 호출자에게 던진다")
    void exceptionDuringSendIsRethrown() {
        // given
        NotificationCommand command = buildCommand(
                NotificationPurpose.VERIFICATION, NotificationChannel.EMAIL, "dedupe-789"
        );
        given(idempotencyGuard.tryAcquire(anyString(), anyString(), any(Duration.class)))
                .willReturn(IdempotencyResult.ACQUIRED);
        willThrow(new RuntimeException("SMTP 연결 실패")).given(emailSender).send(any());

        // when & then - Sync는 예외를 호출자에게 전파해야 함 (트랜잭션 롤백 트리거)
        assertThatThrownBy(() -> dispatcher.dispatch(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("SMTP 연결 실패");

        // 발송 실패 시 반드시 release() 호출 → 재시도 가능 상태로 복구
        // release()가 없으면 다음 요청이 PROCESSING으로 차단됨
        verify(idempotencyGuard).release(anyString(), eq("dedupe-789"));
    }

    @ParameterizedTest(name = "{0} → TTL = {1}")
    @MethodSource("purposeTtlProvider")
    @DisplayName("알림 목적(Purpose)에 따라 올바른 TTL로 tryAcquire가 호출된다")
    void dedupeTtlMatchesPurpose(NotificationPurpose purpose, Duration expectedTtl) {
        // given
        NotificationCommand command = buildCommand(
                purpose, NotificationChannel.EMAIL, "ttl-test-key"
        );
        given(idempotencyGuard.tryAcquire(anyString(), anyString(), any(Duration.class)))
                .willReturn(IdempotencyResult.ACQUIRED);

        // when
        dispatcher.dispatch(command);

        // then - scope는 "notification" 고정, key와 ttl만 검증
        // SCOPE 상수가 바뀌면 테스트가 잡아줌
        verify(idempotencyGuard).tryAcquire(eq("notification"), eq("ttl-test-key"), eq(expectedTtl));
    }

}