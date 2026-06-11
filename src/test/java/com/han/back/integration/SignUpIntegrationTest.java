package com.han.back.integration;

import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.security.token.util.LoginIdTokenUtil;
import jakarta.mail.internet.MimeMessage;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("회원가입 통합 테스트")
class SignUpIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LoginIdTokenUtil loginIdTokenUtil;

    private static final String LOGIN_ID = "newuser01";
    private static final String EMAIL = "newuser@test.com";
    private static final String PASSWORD = "Test1234!";
    private static final String NICKNAME = "신규유저";

    private String checkLoginId(String loginId) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/auth/check-login-id")
                .param("loginId", loginId));
        return objectMapper.readTree(
                result.andReturn().getResponse().getContentAsString()
        ).path("result").path("loginIdToken").asString();
    }

    // LOCAL credential 존재 여부 — 옛 userRepository.existsByLoginId 대체
    private boolean localCredentialExists(String loginId) {
        return credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, loginId);
    }

    // 이메일로 LOCAL 계정 존재 여부 — 옛 userRepository.existsByEmail 대체
    private boolean localAccountExistsByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(u -> credentialRepository.findByUserIdAndProvider(u.getId(), AuthProvider.LOCAL).isPresent())
                .orElse(false);
    }

    private void sendVerificationCode(String email) throws Exception {
        mockMvc.perform(post("/api/v1/verification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "target", email, "type", "SIGN_UP", "channel", "EMAIL"
                ))));
    }

    private String getStoredVerificationCode(String email) {
        return redisTemplate.opsForValue().get(
                VerificationConst.codeKey(VerificationType.SIGN_UP, email));
    }

    private void confirmVerificationCode(String email, String code) throws Exception {
        mockMvc.perform(post("/api/v1/verification/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "target", email, "code", code, "type", "SIGN_UP"
                ))));
    }

    private void injectConfirmedKey(String email) {
        redisTemplate.opsForValue().set(
                VerificationConst.confirmedKey(VerificationType.SIGN_UP, email), "CONFIRMED");
    }

    private ResultActions requestSignUp(String loginId, String email, String loginIdToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", loginId,
                        "password", PASSWORD,
                        "email", email,
                        "nickname", NICKNAME,
                        "loginIdToken", loginIdToken
                ))));
    }

    @Nested
    @DisplayName("정상 플로우")
    class HappyPath {

        @Test
        @DisplayName("4단계 플로우를 완료하면 user와 LOCAL credential이 DB에 저장되고 Redis confirmed 키가 소비된다")
        void fullSignUpFlow_savesUserAndConsumesConfirmedKey() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);
            assertThat(loginIdToken).isNotEmpty();

            sendVerificationCode(EMAIL);
            String code = getStoredVerificationCode(EMAIL);
            assertThat(code).hasSize(6);

            assertThat(redisTemplate.hasKey(
                    VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL))).isTrue();
            assertThat(redisTemplate.hasKey(
                    VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL))).isTrue();

            confirmVerificationCode(EMAIL, code);

            assertThat(redisTemplate.hasKey(
                    VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL))).isFalse();
            assertThat(redisTemplate.hasKey(
                    VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL))).isTrue();

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            // DB 저장 확인 — user + LOCAL credential
            assertThat(localCredentialExists(LOGIN_ID)).isTrue();
            assertThat(localAccountExistsByEmail(EMAIL)).isTrue();

            // consumeConfirmation이 AFTER_COMMIT 리스너로 동기 실행되어 응답 시점에 이미 소비
            assertThat(redisTemplate.hasKey(
                    VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL))).isFalse();
        }

        @Test
        @DisplayName("회원가입 완료 후 환영 메일 발송이 시도된다")
        void fullSignUpFlow_attemptsWelcomeMailSend() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);
            injectConfirmedKey(EMAIL);

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isOk());

            Awaitility.await()
                    .atMost(5, TimeUnit.SECONDS)
                    .untilAsserted(() ->
                            verify(javaMailSender, atLeast(1)).send(any(MimeMessage.class))
                    );
        }

        @Test
        @DisplayName("회원가입 완료 후 동일 이메일로 재가입 시도하면 VERIFY_NOT_COMPLETED가 반환된다")
        void signUpTwice_withConsumedConfirmedKey_returnsVerifyNotCompleted() throws Exception {
            String token1 = checkLoginId(LOGIN_ID);
            injectConfirmedKey(EMAIL);
            requestSignUp(LOGIN_ID, EMAIL, token1).andExpect(status().isOk());

            String token2 = checkLoginId("newuser02");
            requestSignUp("newuser02", EMAIL, token2)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VERIFY_NOT_COMPLETED"));
        }
    }

    @Nested
    @DisplayName("checkLoginId 실패")
    class CheckLoginIdFailures {

        @Test
        @DisplayName("이미 가입된 loginId로 중복 확인 시 409 ACCOUNT_DUPLICATE_LOGIN_ID가 반환된다")
        void duplicateLoginId_returnsConflict() throws Exception {
            signUpAs(LOGIN_ID, EMAIL);

            mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", LOGIN_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_DUPLICATE_LOGIN_ID"));
        }

        @Test
        @DisplayName("loginId가 빈 값이면 400 VALIDATION_FAIL이 반환된다")
        void emptyLoginId_returnsValidationFail() throws Exception {
            mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAIL"));
        }
    }

    @Nested
    @DisplayName("인증 코드 발송 실패")
    class VerificationSendFailures {

        @Test
        @DisplayName("이미 가입된 LOCAL 이메일로 발송 시 409 ACCOUNT_DUPLICATE_EMAIL이 반환된다")
        void duplicateEmail_returnsConflict() throws Exception {
            signUpAs("existinguser", EMAIL);

            mockMvc.perform(post("/api/v1/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "type", "SIGN_UP", "channel", "EMAIL"
                            ))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_DUPLICATE_EMAIL"));
        }

        @Test
        @DisplayName("60초 내 재발송 시 429 VERIFY_COOLDOWN이 반환된다")
        void cooldownViolation_returnsTooManyRequests() throws Exception {
            sendVerificationCode(EMAIL);

            mockMvc.perform(post("/api/v1/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "type", "SIGN_UP", "channel", "EMAIL"
                            ))))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("VERIFY_COOLDOWN"));
        }

        @Test
        @DisplayName("미지원 채널(SMS) 요청 시 422 VERIFY_CHANNEL_UNSUPPORTED가 반환된다")
        void unsupportedChannel_returnsUnprocessable() throws Exception {
            mockMvc.perform(post("/api/v1/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "type", "SIGN_UP", "channel", "SMS"
                            ))))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("VERIFY_CHANNEL_UNSUPPORTED"));
        }
    }

    @Nested
    @DisplayName("인증 코드 확인 실패")
    class VerificationConfirmFailures {

        @Test
        @DisplayName("틀린 코드 입력 시 400 VERIFY_CODE_MISMATCH가 반환되고 code 키는 유지된다")
        void wrongCode_returnsMismatchAndKeepsCodeKey() throws Exception {
            sendVerificationCode(EMAIL);
            String correctCode = getStoredVerificationCode(EMAIL);

            mockMvc.perform(post("/api/v1/verification/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "code", "000000", "type", "SIGN_UP"
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VERIFY_CODE_MISMATCH"));

            assertThat(getStoredVerificationCode(EMAIL)).isEqualTo(correctCode);
        }
    }

    @Nested
    @DisplayName("signUp 실패")
    class SignUpFailures {

        @Test
        @DisplayName("loginIdToken 필드 없이 회원가입 시 400 VALIDATION_FAIL이 반환된다")
        void missingLoginIdToken_returnsValidationFail() throws Exception {
            injectConfirmedKey(EMAIL);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", LOGIN_ID, "password", PASSWORD,
                                    "email", EMAIL, "nickname", NICKNAME
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAIL"));
        }

        @Test
        @DisplayName("위변조된 loginIdToken으로 회원가입 시 400 AUTH_LOGIN_ID_CHECK_REQUIRED가 반환된다")
        void tamperedLoginIdToken_returnsLoginIdCheckRequired() throws Exception {
            injectConfirmedKey(EMAIL);

            requestSignUp(LOGIN_ID, EMAIL, "aW52YWxpZC50b2tlbg")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AUTH_LOGIN_ID_CHECK_REQUIRED"));
        }

        @Test
        @DisplayName("다른 loginId로 발급된 토큰을 사용하면 400 AUTH_LOGIN_ID_CHECK_REQUIRED가 반환된다")
        void mismatchedLoginIdToken_returnsLoginIdCheckRequired() throws Exception {
            String otherToken = loginIdTokenUtil.issue("otherid");
            injectConfirmedKey(EMAIL);

            requestSignUp(LOGIN_ID, EMAIL, otherToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AUTH_LOGIN_ID_CHECK_REQUIRED"));
        }

        @Test
        @DisplayName("이메일 인증 없이 회원가입 시 400 VERIFY_NOT_COMPLETED가 반환되고 DB에 저장되지 않는다")
        void noEmailVerification_returnsVerifyNotCompletedAndNoDbEntry() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VERIFY_NOT_COMPLETED"));

            assertThat(localCredentialExists(LOGIN_ID)).isFalse();
        }

        @Test
        @DisplayName("시간차로 같은 아이디가 선점되면 409 ACCOUNT_DUPLICATE_LOGIN_ID가 반환된다")
        void loginIdRaceCondition_returnsConflict() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);
            injectConfirmedKey(EMAIL);
            signUpAs(LOGIN_ID, "other@test.com");

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_DUPLICATE_LOGIN_ID"));
        }

        @Test
        @DisplayName("시간차로 같은 이메일이 선점되면 409 ACCOUNT_DUPLICATE_EMAIL이 반환된다")
        void emailRaceCondition_returnsConflict() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);
            injectConfirmedKey(EMAIL);
            signUpAs("otherid", EMAIL);

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_DUPLICATE_EMAIL"));
        }
    }

}