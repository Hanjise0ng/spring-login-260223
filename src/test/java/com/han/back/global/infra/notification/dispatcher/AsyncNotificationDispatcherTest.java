package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationIdempotencyGuard;
import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.infra.notification.NotificationSender;
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
    @Mock private NotificationIdempotencyGuard idempotencyGuard;

    private AsyncNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
        dispatcher = new AsyncNotificationDispatcher(List.of(emailSender), idempotencyGuard);
    }

    // ── 파라미터 소스 ──────────────────────────────────────────────
    // Purpose가 TTL을 소유하므로 enum에서 직접 가져옴
    // → Purpose TTL이 바뀌면 테스트도 자동으로 반영됨
    static Stream<Arguments> purposeTtlProvider() {
        return Stream.of(NotificationPurpose.values())
                .map(p -> Arguments.of(p, p.getDedupeTtl()));
    }

    // ── 테스트 ────────────────────────────────────────────────────

    @Test
    @DisplayName("이미 처리된 중복 요청(tryAcquire=false)이면 발송을 생략한다")
    void skipDuplicateDispatch() {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(request.getDedupeKey()).willReturn("dedupe-123");
        // Duration을 any()로 매칭 — tryAcquire 시그니처가 Duration으로 변경됨
        given(idempotencyGuard.tryAcquire(anyString(), any(Duration.class))).willReturn(false);

        dispatcher.dispatch(request);

        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("요청된 채널에 해당하는 Sender가 없으면 예외 없이 로깅 후 종료한다")
    void noSenderFound() {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.WELCOME);
        given(request.getChannel()).willReturn(NotificationChannel.SMS);
        given(idempotencyGuard.tryAcquire(any(), any(Duration.class))).willReturn(true);

        dispatcher.dispatch(request);

        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("발송 중 예외가 발생해도 Async 환경에서는 예외를 던지지 않고 처리한다")
    void exceptionDuringSendIsCaught() {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(request.getChannel()).willReturn(NotificationChannel.EMAIL);
        given(idempotencyGuard.tryAcquire(any(), any(Duration.class))).willReturn(true);
        willThrow(new RuntimeException("Send Failure")).given(emailSender).send(request);

        dispatcher.dispatch(request);
        // then: 예외가 밖으로 던져지지 않아야 성공
    }

    @ParameterizedTest(name = "{0} → TTL = {1}")
    @MethodSource("purposeTtlProvider")
    @DisplayName("알림 목적(Purpose)에 따라 올바른 TTL이 설정된다")
    void dedupeTtlMatchesPurpose(NotificationPurpose purpose, Duration expectedTtl) {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(purpose);
        given(request.getChannel()).willReturn(NotificationChannel.EMAIL);
        given(request.getDedupeKey()).willReturn("key");
        given(idempotencyGuard.tryAcquire(eq("key"), eq(expectedTtl))).willReturn(true);

        dispatcher.dispatch(request);

        verify(idempotencyGuard).tryAcquire("key", expectedTtl);
    }

}