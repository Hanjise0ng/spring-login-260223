package com.han.back.domain.auth.service.implement;

import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
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
@DisplayName("ExistingEmailPolicy")
class ExistingEmailPolicyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExistingEmailPolicy existingEmailPolicy;

    private static final String EMAIL = "test@test.com";

    @Nested
    @DisplayName("getSupportedTypes()")
    class GetSupportedTypes {

        @Test
        @DisplayName("PASSWORD_RESET 타입만 지원한다")
        void returnPasswordResetOnly() {
            Set<VerificationType> result = existingEmailPolicy.getSupportedTypes();

            assertThat(result).containsExactly(VerificationType.PASSWORD_RESET);
        }
    }

    @Nested
    @DisplayName("check()")
    class Check {

        @Test
        @DisplayName("존재하는 이메일 → 예외 없이 통과한다")
        void existingEmail_passes() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            assertThatCode(() -> existingEmailPolicy.check(EMAIL))
                    .doesNotThrowAnyException();

            then(userRepository).should(times(1)).existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("존재하지 않는 이메일 → NOT_FOUND_USER 예외를 던진다")
        void nonExistingEmail_throwsNotFoundUser() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            assertThatThrownBy(() -> existingEmailPolicy.check(EMAIL))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.NOT_FOUND_USER);
        }
    }

}