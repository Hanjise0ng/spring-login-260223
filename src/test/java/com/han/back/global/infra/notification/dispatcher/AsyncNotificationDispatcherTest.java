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

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsyncNotificationDispatcherTest {

    @Mock private NotificationSender emailSender;
    @Mock private IdempotencyGuard idempotencyGuard;

    private AsyncNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
        dispatcher = new AsyncNotificationDispatcher(List.of(emailSender), idempotencyGuard);
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

        given(idempotencyGuard.tryAcquire(anyString(), anyString(), any(Duration.class)))
                .willReturn(IdempotencyResult.COMPLETED);

        // when
        dispatcher.dispatch(command);

        // then
        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("요청된 채널에 해당하는 Sender가 없으면 예외 없이 로깅 후 종료한다")
    void noSenderFound() {
        // given - SMS 채널로 요청하지만 emailSender만 등록된 상황
        NotificationCommand command = buildCommand(
                NotificationPurpose.WELCOME, NotificationChannel.SMS, "dedupe-456"
        );
        given(idempotencyGuard.tryAcquire(anyString(), anyString(), any(Duration.class)))
                .willReturn(IdempotencyResult.ACQUIRED);

        // when
        dispatcher.dispatch(command);

        // then
        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("발송 중 예외가 발생해도 Async 환경에서는 예외를 밖으로 전파하지 않는다")
    void exceptionDuringSendIsCaught() {
        // given
        NotificationCommand command = buildCommand(
                NotificationPurpose.VERIFICATION, NotificationChannel.EMAIL, "dedupe-789"
        );
        given(idempotencyGuard.tryAcquire(anyString(), anyString(), any(Duration.class)))
                .willReturn(IdempotencyResult.ACQUIRED);
        willThrow(new RuntimeException("SMTP 연결 실패")).given(emailSender).send(any());

        // when & then: 예외가 밖으로 나오지 않으면 통과
        dispatcher.dispatch(command);

        // 실패 시 release()가 호출되는지 추가 검증
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
        // 실제 프로덕션 코드의 SCOPE 상수("notification")와 맞춤
        verify(idempotencyGuard).tryAcquire(eq("notification"), eq("ttl-test-key"), eq(expectedTtl));
    }

}