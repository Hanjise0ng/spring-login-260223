package com.han.back.global.notification.implement;

import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.implement.EmailNotificationSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationSender")
class EmailNotificationSenderTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private MimeMessage mimeMessage;

    @InjectMocks private EmailNotificationSender emailNotificationSender;

    private static final String EMAIL   = "test@example.com";
    private static final String SUBJECT = "[HAN] 회원가입 인증 코드";
    private static final String CONTENT = "<html>123456</html>";

    @Test
    @DisplayName("getChannel() → EMAIL을 반환한다")
    void getChannel_returnsEmail() {
        assertThat(emailNotificationSender.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Nested
    @DisplayName("send()")
    class Send {

        @Test
        @DisplayName("정상 발송 → MimeMessage를 생성하고 JavaMailSender.send()를 호출한다")
        void happyPath_createsMessageAndDispatches() {
            given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);

            assertThatCode(() -> emailNotificationSender.send(EMAIL, SUBJECT, CONTENT))
                    .doesNotThrowAnyException();

            then(javaMailSender).should(times(1)).createMimeMessage();
            then(javaMailSender).should(times(1)).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("MimeMessage 생성 중 MessagingException → MAIL_FAIL 예외를 던진다")
        void createMessageFails_throwsMailFail() {
            // MimeMessage의 어떤 메서드가 호출되든 MessagingException을 던지도록 설정
            MimeMessage faultyMessage = mock(MimeMessage.class, invocation -> {
                throw new MessagingException("forced failure");
            });
            given(javaMailSender.createMimeMessage()).willReturn(faultyMessage);

            assertThatThrownBy(() -> emailNotificationSender.send(EMAIL, SUBJECT, CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.MAIL_FAIL);

            then(javaMailSender).should(never()).send(any(MimeMessage.class));
        }

        @Test
        @DisplayName("메일 발송 실패(MailException) → MAIL_FAIL 예외를 던진다")
        void dispatchFails_throwsMailFail() {
            given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
            willThrow(new MailSendException("Connection refused"))
                    .given(javaMailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailNotificationSender.send(EMAIL, SUBJECT, CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.MAIL_FAIL);
        }

        @Test
        @DisplayName("이메일 로컬파트 2자 이하에서 발송 실패 → maskEmail의 atIndex <= 2 분기를 커버한다")
        void shortEmailLocal_dispatchFails_coversShortMaskBranch() {
            given(javaMailSender.createMimeMessage()).willReturn(mimeMessage);
            willThrow(new MailSendException("timeout"))
                    .given(javaMailSender).send(any(MimeMessage.class));

            assertThatThrownBy(() -> emailNotificationSender.send("ab@example.com", SUBJECT, CONTENT))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.MAIL_FAIL);
        }
    }

}