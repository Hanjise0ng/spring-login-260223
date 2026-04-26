package com.han.back.domain.verification.policy;

import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.UserExistenceChecker;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.response.BaseResponseStatus;
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
@DisplayName("DuplicateTargetPolicy")
class DuplicateTargetPolicyTest {

    @Mock private UserExistenceChecker userExistenceChecker;

    @InjectMocks private DuplicateTargetPolicy duplicateTargetPolicy;

    private static final String EMAIL = "test@test.com";

    @Nested
    @DisplayName("getSupportedTypes()")
    class GetSupportedTypes {

        @Test
        @DisplayName("SIGN_UP과 EMAIL_CHANGE 타입을 지원한다")
        void returnSignUpAndEmailChange() {
            Set<VerificationType> result = duplicateTargetPolicy.getSupportedTypes();

            assertThat(result).containsExactlyInAnyOrder(
                    VerificationType.SIGN_UP,
                    VerificationType.EMAIL_CHANGE
            );
        }
    }

    @Nested
    @DisplayName("check()")
    class Check {

        @Test
        @DisplayName("EMAIL 채널 + 사용 가능한 이메일 → 예외 없이 통과한다")
        void email_available_passes() {
            given(userExistenceChecker.existsByEmail(EMAIL)).willReturn(false);

            assertThatCode(() -> duplicateTargetPolicy.check(EMAIL, NotificationChannel.EMAIL))
                    .doesNotThrowAnyException();

            then(userExistenceChecker).should(times(1)).existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("EMAIL 채널 + 이미 존재하는 이메일 → DUPLICATE_EMAIL 예외를 던진다")
        void email_duplicate_throwsDuplicateEmail() {
            given(userExistenceChecker.existsByEmail(EMAIL)).willReturn(true);

            assertThatThrownBy(() -> duplicateTargetPolicy.check(EMAIL, NotificationChannel.EMAIL))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.DUPLICATE_EMAIL);
        }

        @Test
        @DisplayName("SMS 채널 → UNSUPPORTED_NOTIFICATION_CHANNEL 예외를 던진다")
        void sms_throwsUnsupported() {
            assertThatThrownBy(() -> duplicateTargetPolicy.check("01012345678", NotificationChannel.SMS))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.UNSUPPORTED_NOTIFICATION_CHANNEL);
        }
    }

}