package com.han.back.global.infra.notification.strategy;

import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MailSendUtilTest {

    @Mock private JavaMailSender mailSender;
    @Mock private MimeMessage mimeMessage;
    @Mock private NotificationRequest request;

    @BeforeEach
    void setUp() {
        // MimeMessageHelper의 Assert.notNull 검증(IllegalArgumentException)을
        // 무사히 통과하기 위해 필수 데이터들을 기본적으로 모킹해 둡니다.
        given(mailSender.createMimeMessage()).willReturn(mimeMessage);
        given(request.getTarget()).willReturn("test@test.com");
        given(request.getSubject()).willReturn("Test Subject");
        given(request.getContent()).willReturn("Test Content");
    }

    @Test
    @DisplayName("이메일을 정상적으로 발송한다")
    void sendMimeMessage_Success() {
        MailSendUtil.sendMimeMessage(mailSender, "from@test.com", request);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("MailException 발생 시 그대로 예외를 던진다")
    void sendMimeMessage_MailException() {
        // 실제 발송 단계에서 예외 발생 유도
        willThrow(new MailSendException("Fail")).given(mailSender).send(mimeMessage);

        assertThatThrownBy(() -> MailSendUtil.sendMimeMessage(mailSender, "from@test.com", request))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    @DisplayName("MessagingException 발생 시 CustomException(MAIL_FAIL)을 던진다")
    void sendMimeMessage_MessagingException() throws Exception {
        // MimeMessageHelper가 내부적으로 mimeMessage.setSubject()를 호출할 때 예외 발생 유도
        willThrow(new MessagingException()).given(mimeMessage).setSubject(anyString(), anyString());

        assertThatThrownBy(() -> MailSendUtil.sendMimeMessage(mailSender, "from@test.com", request))
                .isInstanceOf(CustomException.class);
    }

}