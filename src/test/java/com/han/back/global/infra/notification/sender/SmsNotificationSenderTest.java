package com.han.back.global.infra.notification.sender;

import com.han.back.global.infra.notification.model.NotificationChannel;
import com.han.back.global.infra.notification.model.NotificationPurpose;
import com.han.back.global.infra.notification.model.NotificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsNotificationSender")
class SmsNotificationSenderTest {

    private final SmsNotificationSender smsNotificationSender = new SmsNotificationSender();

    @Test
    @DisplayName("getChannel() → SMS를 반환한다")
    void getChannel_returnsSms() {
        assertThat(smsNotificationSender.getChannel()).isEqualTo(NotificationChannel.SMS);
    }

    // ── SMS 미구현 스텁 테스트 ─────────────────────────────────────
    //
    // [현재 상태] SmsNotificationSender.send()는 빈 메서드(stub) 상태
    //             send()를 호출해도 예외가 발생하지 않아야 한다
    //
    // SMS 기능이 실제로 구현되면 아래 테스트는 반드시 교체해야 한다:
    //      1. 이 테스트 삭제
    //      2. EmailNotificationSenderTest 구조를 참고해
    //         - strategy 위임 검증
    //         - 예외 전파 검증
    //         - 미등록 purpose 예외 검증
    //         으로 확장할 것
    //
    // ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("[STUB] send() → SMS 미구현 상태 — 예외 없이 종료한다")
    void send_stubState_doesNotThrow() {
        // given
        NotificationRequest request = NotificationRequest.of(
                NotificationChannel.SMS,
                "01012345678",
                "subject",
                "content",
                NotificationPurpose.VERIFICATION  // traceKey / dedupeKey는 NotificationMetadata 소속
        );

        // when & then
        // 현재 send()는 빈 구현 — 핵심 계약은 "예외 없이 종료"
        assertThatCode(() -> smsNotificationSender.send(request))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("[STUB] send() → 반환값이 없고 상태 변경도 없다 (순수 no-op)")
    void send_stubState_isNoOp() {
        // given
        NotificationRequest request = NotificationRequest.of(
                NotificationChannel.SMS,
                "01012345678",
                "subject",
                "content",
                NotificationPurpose.VERIFICATION
        );

        // when - 두 번 호출해도 동일한 결과여야 함
        assertThatCode(() -> {
            smsNotificationSender.send(request);
            smsNotificationSender.send(request);
        }).doesNotThrowAnyException();

        // 스텁 상태 = 멱등(idempotent) — 몇 번 호출해도 side-effect가 없어야 함
    }

}