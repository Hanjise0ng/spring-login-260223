package com.han.back.domain.verification.policy;

import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.domain.verification.service.UserExistencePort;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.model.NotificationChannel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExistingTargetPolicy")
class ExistingTargetPolicyTest {

    @Mock private UserExistencePort userExistencePort;

    @InjectMocks private ExistingTargetPolicy existingTargetPolicy;

    private static final String EMAIL = "test@test.com";

    @Nested
    @DisplayName("getSupportedTypes()")
    class GetSupportedTypes {

        @Test
        @DisplayName("PASSWORD_RESET 타입만 지원한다")
        void returnPasswordResetOnly() {
            Set<VerificationType> result = existingTargetPolicy.getSupportedTypes();

            assertThat(result).containsExactly(VerificationType.PASSWORD_RESET);
        }
    }

    @Nested
    @DisplayName("check()")
    class Check {

        @Test
        @DisplayName("EMAIL 채널 + 존재하는 이메일 → 예외 없이 통과한다")
        void email_existing_passes() {
            given(userExistencePort.existsByEmail(EMAIL)).willReturn(true);

            assertThatCode(() -> existingTargetPolicy.check(EMAIL, NotificationChannel.EMAIL))
                    .doesNotThrowAnyException();

            then(userExistencePort).should(times(1)).existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("EMAIL 채널 + 존재하지 않는 이메일 → NOT_FOUND_USER 예외를 던진다")
        void email_nonExisting_throwsNotFoundUser() {
            given(userExistencePort.existsByEmail(EMAIL)).willReturn(false);

            assertThatThrownBy(() -> existingTargetPolicy.check(EMAIL, NotificationChannel.EMAIL))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(AccountResponseStatus.ACCOUNT_USER_NOT_FOUND);
        }

        @Test
        @DisplayName("SMS 채널 → UNSUPPORTED_NOTIFICATION_CHANNEL 예외를 던진다")
        void sms_throwsUnsupported() {
            assertThatThrownBy(() -> existingTargetPolicy.check("01012345678", NotificationChannel.SMS))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_CHANNEL_UNSUPPORTED);
        }
    }

}