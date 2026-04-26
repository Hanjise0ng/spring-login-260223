package com.han.back.domain.user.event;

import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.infra.notification.NotificationDispatcher;
import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("SignUpPostCommitListener")
class SignUpPostCommitListenerTest {

    @MockitoBean private NotificationDispatcher notificationDispatcher;
    @MockitoBean private VerificationService verificationService;

    @Autowired private ApplicationEventPublisher eventPublisher;
    @Autowired private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("커밋 성공 → 인증 플래그 소비 + 환영 메일 디스패치가 실행된다")
    void afterCommit_consumesAndDispatches() {
        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(createEvent(1L, "test@example.com", "홍길동"))
        );

        then(verificationService).should(times(1))
                .consumeConfirmation(eq("test@example.com"), eq(VerificationType.SIGN_UP));

        ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
        then(notificationDispatcher).should(times(1)).dispatch(captor.capture());

        NotificationRequest request = captor.getValue();
        assertThat(request.getPurpose()).isEqualTo(NotificationPurpose.WELCOME);
        assertThat(request.getTarget()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("롤백 → 리스너가 실행되지 않는다 (유령 메일 차단)")
    void afterRollback_nothingExecutes() {
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(createEvent(2L, "rollback@example.com", "롤백"));
            status.setRollbackOnly();
        });

        then(verificationService).should(never()).consumeConfirmation(any(), any());
        then(notificationDispatcher).should(never()).dispatch(any());
    }

    @Test
    @DisplayName("인증 플래그 소비 실패 → 환영 메일 발송은 계속 진행된다 (실패 격리)")
    void consumeFails_dispatchStillExecutes() {
        willThrow(new RuntimeException("Redis 장애"))
                .given(verificationService).consumeConfirmation(any(), any());

        transactionTemplate.executeWithoutResult(status ->
                eventPublisher.publishEvent(createEvent(3L, "fail@example.com", "실패"))
        );

        then(notificationDispatcher).should(times(1)).dispatch(any());
    }

    private UserSignedUpEvent createEvent(Long userId, String email, String nickname) {
        UserEntity user = UserEntity.builder()
                .email(email)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        return UserSignedUpEvent.of(user);
    }

}