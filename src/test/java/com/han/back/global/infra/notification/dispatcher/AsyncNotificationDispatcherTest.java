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
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsyncNotificationDispatcherTest {

    @Mock
    private NotificationSender emailSender;

    @Mock
    private NotificationIdempotencyGuard idempotencyGuard;

    private AsyncNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
        dispatcher = new AsyncNotificationDispatcher(List.of(emailSender), idempotencyGuard);
    }

    @Test
    @DisplayName("이미 처리된 중복 요청(tryAcquire=false)이면 발송을 생략한다")
    void skipDuplicateDispatch() {
        // given
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(request.getDedupeKey()).willReturn("dedupe-123");
        given(idempotencyGuard.tryAcquire(anyString(), anyLong())).willReturn(false);

        // when
        dispatcher.dispatch(request);

        // then
        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("요청된 채널에 해당하는 Sender가 없으면 예외 없이 로깅 후 종료한다")
    void noSenderFound() {
        // given
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.WELCOME);
        given(request.getChannel()).willReturn(NotificationChannel.SMS); // 등록되지 않은 채널
        given(idempotencyGuard.tryAcquire(any(), anyLong())).willReturn(true);

        // when
        dispatcher.dispatch(request);

        // then
        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("발송 중 예외가 발생해도 Async 환경에서는 예외를 던지지 않고 처리한다")
    void exceptionDuringSendIsCaught() {
        // given
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(request.getChannel()).willReturn(NotificationChannel.EMAIL);
        given(idempotencyGuard.tryAcquire(any(), anyLong())).willReturn(true);
        willThrow(new RuntimeException("Send Failure")).given(emailSender).send(request);

        // when
        dispatcher.dispatch(request);

        // then: 예외가 밖으로 던져지지 않아야 성공
    }

    @ParameterizedTest
    @CsvSource({
            "VERIFICATION, 3600",
            "WELCOME, 86400",
            "PASSWORD_RESET, 3600"
    })
    @DisplayName("알림 목적(Purpose)에 따라 올바른 TTL이 설정된다")
    void selectDedupeTtlByPurpose(NotificationPurpose purpose, long expectedTtl) {
        // given
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(purpose);
        given(request.getChannel()).willReturn(NotificationChannel.EMAIL);
        given(request.getDedupeKey()).willReturn("key");
        given(idempotencyGuard.tryAcquire(eq("key"), eq(expectedTtl))).willReturn(true);

        // when
        dispatcher.dispatch(request);

        // then
        verify(idempotencyGuard).tryAcquire("key", expectedTtl);
    }

}