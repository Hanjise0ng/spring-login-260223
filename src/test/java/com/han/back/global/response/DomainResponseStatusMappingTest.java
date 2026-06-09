package com.han.back.global.response;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.device.exception.DeviceResponseStatus;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.*;

@DisplayName("도메인 ApiResponseStatus enum 매핑")
class DomainResponseStatusMappingTest {

    private static Stream<ApiResponseStatus> allStatuses() {
        return Stream.of(
                ResponseStatus.values(),
                AuthResponseStatus.values(),
                AccountResponseStatus.values(),
                VerificationResponseStatus.values(),
                SocialResponseStatus.values(),
                DeviceResponseStatus.values()
        ).flatMap(Arrays::stream);
    }

    @Nested
    @DisplayName("code-message 일관성")
    class MessageRule {

        @ParameterizedTest
        @MethodSource("com.han.back.global.response.DomainResponseStatusMappingTest#allStatuses")
        @DisplayName("message는 비어 있지 않다")
        void messageIsNotBlank(ApiResponseStatus status) {
            assertThat(status.getMessage()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("code-httpStatus 매핑")
    class HttpStatusMapping {

        private static final Map<String, Object> EXPECTED = Map.ofEntries(
                Map.entry("SUCCESS", OK),
                Map.entry("VALIDATION_FAIL", BAD_REQUEST),
                Map.entry("MALFORMED_REQUEST_BODY", BAD_REQUEST),
                Map.entry("FORBIDDEN", FORBIDDEN),
                Map.entry("RESOURCE_NOT_FOUND", NOT_FOUND),
                Map.entry("METHOD_NOT_ALLOWED", METHOD_NOT_ALLOWED),
                Map.entry("UNSUPPORTED_MEDIA_TYPE", UNSUPPORTED_MEDIA_TYPE),
                Map.entry("RATE_LIMIT_EXCEEDED", TOO_MANY_REQUESTS),
                Map.entry("DB_ERROR", INTERNAL_SERVER_ERROR),
                Map.entry("REDIS_ERROR", INTERNAL_SERVER_ERROR),
                Map.entry("SERIALIZATION_ERROR", INTERNAL_SERVER_ERROR),
                Map.entry("INTERNAL_SERVER_ERROR", INTERNAL_SERVER_ERROR),

                Map.entry("AUTH_LOGIN_ID_CHECK_REQUIRED", BAD_REQUEST),
                Map.entry("AUTH_SIGN_IN_FAIL", UNAUTHORIZED),
                Map.entry("AUTH_AUTHENTICATION_FAIL", UNAUTHORIZED),
                Map.entry("AUTH_INVALID_PASSWORD", UNAUTHORIZED),
                Map.entry("AUTH_JWT_SIGNATURE_INVALID", UNAUTHORIZED),
                Map.entry("AUTH_ACCESS_TOKEN_EXPIRED", UNAUTHORIZED),
                Map.entry("AUTH_JWT_UNSUPPORTED", UNAUTHORIZED),
                Map.entry("AUTH_JWT_EMPTY", UNAUTHORIZED),
                Map.entry("AUTH_ACCESS_TOKEN_MISSING", UNAUTHORIZED),
                Map.entry("AUTH_REFRESH_TOKEN_MISSING", UNAUTHORIZED),
                Map.entry("AUTH_REFRESH_TOKEN_EXPIRED", UNAUTHORIZED),
                Map.entry("AUTH_REFRESH_TOKEN_INVALID", UNAUTHORIZED),

                Map.entry("ACCOUNT_PASSWORD_SAME_AS_CURRENT", BAD_REQUEST),
                Map.entry("ACCOUNT_PASSWORD_CONFIRM_MISMATCH", BAD_REQUEST),
                Map.entry("ACCOUNT_SOCIAL_ONLY", BAD_REQUEST),
                Map.entry("ACCOUNT_TAG_GENERATION_FAIL", BAD_REQUEST),
                Map.entry("ACCOUNT_PASSWORD_RESET_TOKEN_INVALID", UNAUTHORIZED),
                Map.entry("ACCOUNT_STEP_UP_REQUIRED", FORBIDDEN),
                Map.entry("ACCOUNT_USER_NOT_FOUND", NOT_FOUND),
                Map.entry("ACCOUNT_DUPLICATE_LOGIN_ID", CONFLICT),
                Map.entry("ACCOUNT_DUPLICATE_EMAIL", CONFLICT),
                Map.entry("ACCOUNT_ALREADY_DELETED", CONFLICT),
                Map.entry("ACCOUNT_NICKNAME_TAG_DUPLICATE", CONFLICT),

                Map.entry("VERIFY_CODE_MISMATCH", BAD_REQUEST),
                Map.entry("VERIFY_CODE_EXPIRED", BAD_REQUEST),
                Map.entry("VERIFY_NOT_COMPLETED", BAD_REQUEST),
                Map.entry("VERIFY_CHANNEL_UNSUPPORTED", UNPROCESSABLE_CONTENT),
                Map.entry("VERIFY_COOLDOWN", TOO_MANY_REQUESTS),
                Map.entry("VERIFY_MAIL_TEMPLATE_FAIL", INTERNAL_SERVER_ERROR),
                Map.entry("VERIFY_MAIL_SEND_FAIL", INTERNAL_SERVER_ERROR),
                Map.entry("VERIFY_SMS_SEND_FAIL", INTERNAL_SERVER_ERROR),

                Map.entry("SOCIAL_SIGNUP_TOKEN_INVALID", BAD_REQUEST),
                Map.entry("SOCIAL_ACCOUNT_NOT_FOUND", NOT_FOUND),
                Map.entry("SOCIAL_ALREADY_LINKED", CONFLICT),
                Map.entry("SOCIAL_EMAIL_CONFLICT", CONFLICT),
                Map.entry("SOCIAL_PROVIDER_UNSUPPORTED", UNPROCESSABLE_CONTENT),

                Map.entry("DEVICE_SELF_LOGOUT_FORBIDDEN", BAD_REQUEST),
                Map.entry("DEVICE_BANNED", FORBIDDEN),
                Map.entry("DEVICE_NOT_FOUND", NOT_FOUND),
                Map.entry("DEVICE_ACTIVE_DELETE_FORBIDDEN", CONFLICT),
                Map.entry("DEVICE_TRUSTED_LIMIT_EXCEEDED", CONFLICT)
        );

        @ParameterizedTest
        @MethodSource("com.han.back.global.response.DomainResponseStatusMappingTest#allStatuses")
        @DisplayName("각 code의 httpStatus 확인")
        void httpStatusMatchesSpec(ApiResponseStatus status) {
            assertThat(EXPECTED)
                    .as("매핑표에 정의되지 않은 code: %s", status.getCode())
                    .containsKey(status.getCode());
            assertThat(status.getHttpStatus()).isEqualTo(EXPECTED.get(status.getCode()));
        }

        @Test
        @DisplayName("매핑표의 모든 code가 실제 enum으로 구현돼 있다 (누락 방지)")
        void allSpecCodesAreImplemented() {
            List<String> implemented = allStatuses().map(ApiResponseStatus::getCode).toList();
            assertThat(implemented).containsAll(EXPECTED.keySet());
        }
    }

}