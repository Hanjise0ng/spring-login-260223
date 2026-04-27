package com.han.back.global.infra.notification.dispatcher;

import com.han.back.global.infra.notification.*;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SyncNotificationDispatcherTest {

    @Mock private NotificationSender emailSender;
    @Mock private NotificationIdempotencyGuard idempotencyGuard;
    private SyncNotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
        dispatcher = new SyncNotificationDispatcher(List.of(emailSender), idempotencyGuard);
    }

    @Test
    @DisplayName("이미 처리된 중복 요청이면 발송을 생략한다")
    void skipDuplicateDispatch() {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(request.getDedupeKey()).willReturn("key");
        given(idempotencyGuard.tryAcquire(anyString(), anyLong())).willReturn(false);

        dispatcher.dispatch(request);

        verify(emailSender, never()).send(any());
    }

    @Test
    @DisplayName("등록되지 않은 채널이면 IllegalStateException을 던진다")
    void noSenderFoundThrowsException() {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.WELCOME);
        given(request.getChannel()).willReturn(NotificationChannel.SMS);
        given(idempotencyGuard.tryAcquire(any(), anyLong())).willReturn(true);

        assertThatThrownBy(() -> dispatcher.dispatch(request))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("발송 중 예외가 발생하면 Sync 환경에서는 예외를 밖으로 던진다")
    void exceptionDuringSendIsThrown() {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(request.getChannel()).willReturn(NotificationChannel.EMAIL);
        given(idempotencyGuard.tryAcquire(any(), anyLong())).willReturn(true);
        willThrow(new RuntimeException("Send error")).given(emailSender).send(request);

        assertThatThrownBy(() -> dispatcher.dispatch(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Send error");
    }

    @ParameterizedTest
    @CsvSource({"VERIFICATION, 3600", "WELCOME, 86400", "PASSWORD_RESET, 3600"})
    @DisplayName("목적(Purpose)에 따라 올바른 TTL을 설정한다")
    void selectDedupeTtlByPurpose(NotificationPurpose purpose, long expectedTtl) {
        NotificationRequest request = mock(NotificationRequest.class);
        given(request.getPurpose()).willReturn(purpose);
        given(request.getChannel()).willReturn(NotificationChannel.EMAIL);
        given(request.getDedupeKey()).willReturn("key");
        given(idempotencyGuard.tryAcquire(eq("key"), eq(expectedTtl))).willReturn(true);

        dispatcher.dispatch(request);

        verify(idempotencyGuard).tryAcquire("key", expectedTtl);
    }

}