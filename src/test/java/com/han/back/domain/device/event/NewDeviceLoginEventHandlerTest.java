package com.han.back.domain.device.event;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.entity.DeviceType;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NewDeviceLoginEventHandler")
class NewDeviceLoginEventHandlerTest {

    @MockitoBean private NotificationDispatcher notificationDispatcher;
    @MockitoBean private MailTemplateUtil mailTemplateUtil;

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;

    private static final Long   USER_ID     = 1L;
    private static final String EMAIL       = "test@example.com";
    private static final String NICKNAME    = "홍길동";
    private static final String FINGERPRINT = "fp-test-uuid";
    private static final String OS_NAME     = "Windows 10";
    private static final String LOGIN_IP    = "192.168.0.1";
    private static final String BROWSER_NAME = "Chrome";

    @BeforeEach
    void setUp() {
        given(mailTemplateUtil.buildNewDeviceLoginEmail(any(), any(), any(), any(), any()))
                .willReturn("<html>new-device</html>");
    }

    private NewDeviceLoginEvent createEvent(Long userId, String email, String nickname) {
        DeviceInfo deviceInfo = DeviceInfo.of(
                DeviceType.WEB_DESKTOP,
                OS_NAME,
                BROWSER_NAME,
                FINGERPRINT,
                LOGIN_IP
        );
        return NewDeviceLoginEvent.of(userId, email, nickname, deviceInfo);
    }

    // ──────────────────────────────────────────────────────────────
    // 트랜잭션 안에서 이벤트 발행 → AFTER_COMMIT 리스너 트리거
    // ──────────────────────────────────────────────────────────────
    private void publishInTransaction(NewDeviceLoginEvent event) {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(event)
        );
    }

    @Nested
    @DisplayName("트랜잭션 커밋 후 (AFTER_COMMIT)")
    class AfterCommit {

        @Test
        @DisplayName("신규 기기 로그인 이벤트 발행 → 알림 디스패처가 1회 호출된다")
        void newDeviceLoginEvent_dispatchesOnce() {
            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));

            then(notificationDispatcher).should(times(1)).dispatch(any());
        }

        @Test
        @DisplayName("NotificationCommand — channel/purpose/target/subject/dedupeKey가 올바르게 설정된다")
        void dispatches_withCorrectCommandFields() {
            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));

            ArgumentCaptor<NotificationCommand> captor =
                    ArgumentCaptor.forClass(NotificationCommand.class);
            then(notificationDispatcher).should(times(1)).dispatch(captor.capture());

            NotificationCommand command = captor.getValue();

            // request 검증
            assertThat(command.getRequest().getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(command.getRequest().getPurpose()).isEqualTo(NotificationPurpose.NEW_DEVICE_LOGIN);
            assertThat(command.getRequest().getTarget()).isEqualTo(EMAIL);
            assertThat(command.getRequest().getSubject())
                    .isEqualTo(String.format("[HAN] %s님, 새로운 기기에서 로그인되었습니다", NICKNAME));

            // metadata 검증 — userId + fingerprint 조합 dedupeKey
            // keyPolicy.newDeviceLogin(userId, fingerprint) = "new-device-login:user:{id}:fp:{fp}"
            assertThat(command.getMetadata().getDedupeKey())
                    .isEqualTo("new-device-login:user:" + USER_ID + ":fp:" + FINGERPRINT);
        }

        @Test
        @DisplayName("MailTemplateUtil — 이벤트 값이 올바르게 전달된다")
        void templateUtil_calledWithCorrectArgs() {
            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));

            then(mailTemplateUtil).should(times(1)).buildNewDeviceLoginEmail(
                    eq(NICKNAME),
                    eq(DeviceType.WEB_DESKTOP),
                    eq(OS_NAME),
                    eq(LOGIN_IP),
                    any(LocalDateTime.class)
            );
        }

        @Test
        @DisplayName("메일 발송 실패 → 예외를 삼키고 밖으로 전파하지 않는다 (로그인 완료 보호)")
        void dispatchFails_exceptionIsNotPropagated() {
            willThrow(new RuntimeException("SMTP 장애"))
                    .given(notificationDispatcher).dispatch(any());

            // 예외가 밖으로 나오지 않으면 통과
            // 로그인 트랜잭션은 이미 커밋됐으므로 알림 실패가 로그인 결과에 영향 주면 안 됨
            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));
        }

        @Test
        @DisplayName("MailTemplateUtil 실패 → 예외를 삼키고 디스패치는 호출되지 않는다")
        void templateBuildFails_dispatchNotCalled() {
            willThrow(new RuntimeException("템플릿 로드 실패"))
                    .given(mailTemplateUtil).buildNewDeviceLoginEmail(any(), any(), any(), any(), any());

            publishInTransaction(createEvent(USER_ID, EMAIL, NICKNAME));

            then(notificationDispatcher).should(never()).dispatch(any());
        }
    }

    @Nested
    @DisplayName("트랜잭션 롤백 후 (AFTER_ROLLBACK)")
    class AfterRollback {

        @Test
        @DisplayName("롤백 → 리스너가 실행되지 않아 알림이 발송되지 않는다")
        void rollback_dispatchNotCalled() {
            transactionTemplate.executeWithoutResult(status -> {
                eventPublisher.publishEvent(createEvent(2L, "rollback@example.com", "롤백유저"));
                status.setRollbackOnly();
            });

            then(notificationDispatcher).should(never()).dispatch(any());
        }
    }

}