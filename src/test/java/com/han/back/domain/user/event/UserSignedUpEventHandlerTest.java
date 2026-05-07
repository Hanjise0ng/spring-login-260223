package com.han.back.domain.user.event;

import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.infra.notification.dispatcher.NotificationDispatcher;
import com.han.back.global.infra.notification.model.NotificationChannel;
import com.han.back.global.infra.notification.model.NotificationCommand;
import com.han.back.global.infra.notification.model.NotificationPurpose;
import com.han.back.global.infra.notification.template.MailTemplateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("UserSignedUpEventHandler")
class UserSignedUpEventHandlerTest {

    @MockitoBean private NotificationDispatcher notificationDispatcher;
    @MockitoBean private VerificationService verificationService;
    @MockitoBean private MailTemplateUtil mailTemplateUtil;

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;

    private static final Long   USER_ID  = 1L;
    private static final String EMAIL    = "test@example.com";
    private static final String NICKNAME = "홍길동";

    @BeforeEach
    void setUp() {
        // MailTemplateUtil이 mock이므로 반환값을 명시해야 dispatchWelcomeMail 내부가 정상 진행됨
        given(mailTemplateUtil.buildWelcomeEmail(any(), any()))
                .willReturn("<html>welcome</html>");
    }

    private UserSignedUpEvent createEvent(Long userId, String email, String nickname) {
        UserEntity user = UserEntity.builder()
                .email(email)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return UserSignedUpEvent.of(user);
    }

    @Nested
    @DisplayName("트랜잭션 커밋 후 (AFTER_COMMIT)")
    class AfterCommit {

        @Test
        @DisplayName("인증 플래그 소비 → SIGN_UP 타입으로 consumeConfirmation이 1회 호출된다")
        void consumesVerificationFlag() {
            // when
            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));

            // then
            then(verificationService).should(times(1))
                    .consumeConfirmation(eq(EMAIL), eq(VerificationType.SIGN_UP));
        }

        @Test
        @DisplayName("환영 메일 디스패치 → NotificationCommand의 channel/purpose/target/subject/dedupeKey가 올바르게 설정된다")
        void dispatches_withCorrectCommandFields() {
            // when
            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));

            // then — NotificationCommand 전체를 캡처해 내부 필드 검증
            ArgumentCaptor<NotificationCommand> captor =
                    ArgumentCaptor.forClass(NotificationCommand.class);
            then(notificationDispatcher).should(times(1)).dispatch(captor.capture());

            NotificationCommand command = captor.getValue();

            // request 필드 검증
            assertThat(command.getRequest().getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(command.getRequest().getPurpose()).isEqualTo(NotificationPurpose.WELCOME);
            assertThat(command.getRequest().getTarget()).isEqualTo(EMAIL);
            assertThat(command.getRequest().getSubject())
                    .isEqualTo(String.format("[HAN] %s님, 가입을 환영합니다", NICKNAME));

            // metadata 필드 검증 — dedupeKey가 없으면 idempotency 키 설정 버그를 잡지 못함
            // keyPolicy.welcome(userId) = "welcome:user:{userId}"
            assertThat(command.getMetadata().getDedupeKey())
                    .isEqualTo("welcome:user:" + USER_ID);
        }

        @Test
        @DisplayName("인증 플래그 소비 실패 → 예외를 삼키고 환영 메일 디스패치는 계속 진행된다 (실패 격리)")
        void consumeFails_dispatchStillExecutes() {
            // given - Redis 장애 시뮬레이션
            willThrow(new RuntimeException("Redis 장애"))
                    .given(verificationService).consumeConfirmation(any(), any());

            // when
            publishInTransaction(createEvent(USER_ID, "fail@example.com", NICKNAME));

            // then - 소비 실패가 메일 발송을 막으면 안 됨
            then(notificationDispatcher).should(times(1)).dispatch(any());
        }

        @Test
        @DisplayName("환영 메일 발송 실패 → 예외를 삼키고 밖으로 전파하지 않는다 (가입 완료 보호)")
        void dispatchFails_exceptionIsNotPropagated() {
            // given - 메일 발송 실패 시뮬레이션
            willThrow(new RuntimeException("SMTP 장애"))
                    .given(notificationDispatcher).dispatch(any());

            // when & then - 예외가 밖으로 나오면 가입 완료 후 후처리가 실패한 것처럼 보임
            // TransactionalEventListener는 커밋 이후 호출 → 이 예외는 절대 사용자에게 전달되면 안 됨
            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));
            // 예외 없이 통과하면 성공
        }
    }

    @Nested
    @DisplayName("트랜잭션 롤백 후 (AFTER_ROLLBACK)")
    class AfterRollback {

        @Test
        @DisplayName("롤백 → 리스너가 실행되지 않아 유령 메일과 인증 플래그 소비가 차단된다")
        void rollback_nothingExecutes() {
            // when
            transactionTemplate.executeWithoutResult(status -> {
                eventPublisher.publishEvent(createEvent(2L, "rollback@example.com", "롤백"));
                status.setRollbackOnly(); // 강제 롤백
            });

            // then - AFTER_COMMIT 리스너이므로 롤백 시 실행되면 안 됨
            then(verificationService).should(never()).consumeConfirmation(any(), any());
            then(notificationDispatcher).should(never()).dispatch(any());
        }
    }

    private void publishInTransaction(UserSignedUpEvent event) {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(event)
        );
    }

}