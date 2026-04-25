package com.han.back.global.notification.implement;

import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.infra.notification.implement.EmailNotificationSender;
import com.han.back.global.infra.notification.strategy.MailSendStrategy;
import com.han.back.global.response.BaseResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationSender")
class EmailNotificationSenderTest {

    private MailSendStrategy verificationStrategy;
    private MailSendStrategy welcomeStrategy;
    private EmailNotificationSender emailNotificationSender;

    @BeforeEach
    void setUp() {
        verificationStrategy = mock(MailSendStrategy.class);
        welcomeStrategy = mock(MailSendStrategy.class);

        given(verificationStrategy.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(welcomeStrategy.getPurpose()).willReturn(NotificationPurpose.WELCOME);

        emailNotificationSender = new EmailNotificationSender(
                List.of(verificationStrategy, welcomeStrategy));
    }

    @Test
    @DisplayName("getChannel() → EMAIL을 반환한다")
    void getChannel_returnsEmail() {
        assertThat(emailNotificationSender.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Nested
    @DisplayName("send()")
    class Send {

        @Test
        @DisplayName("VERIFICATION purpose → VerificationMailStrategy에 위임한다")
        void verification_delegatesToCorrectStrategy() {
            NotificationRequest request = buildRequest(NotificationPurpose.VERIFICATION);

            emailNotificationSender.send(request);

            then(verificationStrategy).should(times(1)).send(request);
            then(welcomeStrategy).should(never()).send(any());
        }

        @Test
        @DisplayName("WELCOME purpose → WelcomeMailStrategy에 위임한다")
        void welcome_delegatesToCorrectStrategy() {
            NotificationRequest request = buildRequest(NotificationPurpose.WELCOME);

            emailNotificationSender.send(request);

            then(welcomeStrategy).should(times(1)).send(request);
            then(verificationStrategy).should(never()).send(any());
        }

        @Test
        @DisplayName("Strategy에서 MailException 발생 → 호출자에게 그대로 전파한다")
        void strategyThrows_propagatesToCaller() {
            NotificationRequest request = buildRequest(NotificationPurpose.VERIFICATION);
            willThrow(new MailSendException("Connection refused"))
                    .given(verificationStrategy).send(request);

            assertThatThrownBy(() -> emailNotificationSender.send(request))
                    .isInstanceOf(MailSendException.class);
        }

        @Test
        @DisplayName("등록되지 않은 purpose → INTERNAL_SERVER_ERROR 예외를 던진다")
        void unknownPurpose_throwsException() {
            // PASSWORD_RESET strategy 가 등록되지 않은 상태
            NotificationRequest request = buildRequest(NotificationPurpose.PASSWORD_RESET);

            assertThatThrownBy(() -> emailNotificationSender.send(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private NotificationRequest buildRequest(NotificationPurpose purpose) {
        return NotificationRequest.of(
                NotificationChannel.EMAIL,
                "test@example.com",
                "[HAN] 테스트",
                "<html>content</html>",
                purpose,
                "test-trace"
        );
    }

}