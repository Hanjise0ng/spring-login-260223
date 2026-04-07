package com.han.back.domain.auth.service.implement;

import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.dto.BaseResponseStatus;
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
@DisplayName("DuplicateEmailPolicy")
class DuplicateEmailPolicyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DuplicateEmailPolicy duplicateEmailPolicy;

    private static final String EMAIL = "test@test.com";

    @Nested
    @DisplayName("getSupportedTypes()")
    class GetSupportedTypes {

        @Test
        @DisplayName("SIGN_UP과 EMAIL_CHANGE 타입을 지원한다")
        void returnSignUpAndEmailChange() {
            Set<VerificationType> result = duplicateEmailPolicy.getSupportedTypes();

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
        @DisplayName("사용 가능한 이메일 → 예외 없이 통과한다")
        void availableEmail_passes() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);

            assertThatCode(() -> duplicateEmailPolicy.check(EMAIL))
                    .doesNotThrowAnyException();

            then(userRepository).should(times(1)).existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("이미 존재하는 이메일 → DUPLICATE_EMAIL 예외를 던진다")
        void duplicateEmail_throwsDuplicateEmail() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            assertThatThrownBy(() -> duplicateEmailPolicy.check(EMAIL))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.DUPLICATE_EMAIL);
        }
    }

}