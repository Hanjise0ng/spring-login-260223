package com.han.back.global.response;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.device.exception.DeviceResponseStatus;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("발행 code 계약 (외부 공개 후 변경 시 프론트 동시 갱신 필요)")
class PublishedCodeContractTest {

    private static final Set<String> FROZEN_PUBLISHED_CODES = Set.of(
            "SUCCESS",
            "VALIDATION_FAIL",
            "MALFORMED_REQUEST_BODY",
            "FORBIDDEN",
            "RESOURCE_NOT_FOUND",
            "METHOD_NOT_ALLOWED",
            "UNSUPPORTED_MEDIA_TYPE",
            "RATE_LIMIT_EXCEEDED",
            "DB_ERROR",
            "REDIS_ERROR",
            "SERIALIZATION_ERROR",
            "INTERNAL_SERVER_ERROR",

            "AUTH_LOGIN_ID_CHECK_REQUIRED",
            "AUTH_SIGN_IN_FAIL",
            "AUTH_AUTHENTICATION_FAIL",
            "AUTH_INVALID_PASSWORD",
            "AUTH_JWT_SIGNATURE_INVALID",
            "AUTH_ACCESS_TOKEN_EXPIRED",
            "AUTH_JWT_UNSUPPORTED",
            "AUTH_JWT_EMPTY",
            "AUTH_ACCESS_TOKEN_MISSING",
            "AUTH_REFRESH_TOKEN_MISSING",
            "AUTH_REFRESH_TOKEN_EXPIRED",
            "AUTH_REFRESH_TOKEN_INVALID",

            "ACCOUNT_PASSWORD_SAME_AS_CURRENT",
            "ACCOUNT_PASSWORD_CONFIRM_MISMATCH",
            "ACCOUNT_SOCIAL_ONLY",
            "ACCOUNT_TAG_GENERATION_FAIL",
            "ACCOUNT_PASSWORD_RESET_TOKEN_INVALID",
            "ACCOUNT_STEP_UP_REQUIRED",
            "ACCOUNT_USER_NOT_FOUND",
            "ACCOUNT_DUPLICATE_LOGIN_ID",
            "ACCOUNT_DUPLICATE_EMAIL",
            "ACCOUNT_ALREADY_DELETED",
            "ACCOUNT_NICKNAME_TAG_DUPLICATE",

            "VERIFY_CODE_MISMATCH",
            "VERIFY_CODE_EXPIRED",
            "VERIFY_NOT_COMPLETED",
            "VERIFY_CHANNEL_UNSUPPORTED",
            "VERIFY_COOLDOWN",
            "VERIFY_MAIL_TEMPLATE_FAIL",
            "VERIFY_MAIL_SEND_FAIL",
            "VERIFY_SMS_SEND_FAIL",

            "SOCIAL_SIGNUP_TOKEN_INVALID",
            "SOCIAL_ACCOUNT_NOT_FOUND",
            "SOCIAL_ALREADY_LINKED",
            "SOCIAL_PROVIDER_UNSUPPORTED",

            "DEVICE_SELF_LOGOUT_FORBIDDEN",
            "DEVICE_BANNED",
            "DEVICE_NOT_FOUND",
            "DEVICE_ACTIVE_DELETE_FORBIDDEN",
            "DEVICE_TRUSTED_LIMIT_EXCEEDED"
    );

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

    @Test
    @DisplayName("발행 code 집합은 동결 스냅샷과 정확히 일치한다")
    void publishedCodesMatchFrozenSnapshot() {
        Set<String> actual = allStatuses()
                .map(ApiResponseStatus::getCode)
                .collect(Collectors.toUnmodifiableSet());

        assertThat(actual)
                .as("발행 code가 동결 스냅샷과 다르다. code는 기획서 표·프론트 분기 키의 식별자이므로, "
                        + "정당한 추가/변경이면 기획서와 프론트를 함께 갱신한 뒤 이 스냅샷을 의도적으로 갱신하라.")
                .isEqualTo(FROZEN_PUBLISHED_CODES);
    }

    @ParameterizedTest
    @MethodSource("com.han.back.global.response.PublishedCodeContractTest#allStatuses")
    @DisplayName("발행 code는 상수명과 일치한다 (name = code 정책)")
    void codeEqualsConstantName(ApiResponseStatus status) {
        assertThat(status).isInstanceOf(Enum.class);

        assertThat(status.getCode())
                .as("상수 [%s]의 code가 상수명과 다르다. name=code 정책 위반 — 상수명 리네이밍 시 code 리터럴도 함께 갱신하라.",
                        ((Enum<?>) status).name())
                .isEqualTo(((Enum<?>) status).name());
    }

    @ParameterizedTest
    @MethodSource("com.han.back.global.response.PublishedCodeContractTest#allStatuses")
    @DisplayName("발행 code는 전역 유일하다 (클라이언트 분기 키 충돌 방지)")
    void publishedCodesAreGloballyUnique(ApiResponseStatus status) {
        long count = allStatuses()
                .map(ApiResponseStatus::getCode)
                .filter(code -> code.equals(status.getCode()))
                .count();

        assertThat(count)
                .as("code [%s]가 중복 발행됐다", status.getCode())
                .isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("com.han.back.global.response.PublishedCodeContractTest#allStatuses")
    @DisplayName("발행 code는 자기설명 형식이다 (Common 외 도메인 prefix, 5자 이상)")
    void publishedCodeFollowsNamingRule(ApiResponseStatus status) {
        String code = status.getCode();

        assertThat(code).hasSizeGreaterThanOrEqualTo(5);

        if (status instanceof ResponseStatus) {
            assertThat(code)
                    .as("Common enum은 도메인 prefix를 쓰지 않는다 (code=%s)", code)
                    .matches("^[A-Z][A-Z_]*$")
                    .doesNotStartWith("AUTH_")
                    .doesNotStartWith("ACCOUNT_")
                    .doesNotStartWith("VERIFY_")
                    .doesNotStartWith("SOCIAL_")
                    .doesNotStartWith("DEVICE_");
        } else {
            assertThat(code)
                    .as("도메인 enum은 {DOMAIN}_ prefix를 가져야 한다 (code=%s)", code)
                    .matches("^(AUTH|ACCOUNT|VERIFY|SOCIAL|DEVICE)_[A-Z_]+$");
        }
    }

}