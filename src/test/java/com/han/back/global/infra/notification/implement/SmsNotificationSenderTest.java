package com.han.back.global.infra.notification.implement;

import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("SmsNotificationSender")
class SmsNotificationSenderTest {

    private final SmsNotificationSender smsNotificationSender = new SmsNotificationSender();

    @Test
    @DisplayName("getChannel() → SMS를 반환한다")
    void getChannel_returnsSms() {
        assertThat(smsNotificationSender.getChannel()).isEqualTo(NotificationChannel.SMS);
    }

    @Test
    @DisplayName("send() → 현재 미구현 상태이므로 예외 없이 통과한다")
    void send_doesNotThrow() {
        NotificationRequest request = NotificationRequest.of(
                NotificationChannel.SMS, "01012345678", "subject", "content",
                NotificationPurpose.VERIFICATION, "test-trace", "test-dedupe"
        );

        assertThatCode(() -> smsNotificationSender.send(request))
                .doesNotThrowAnyException();
    }

}