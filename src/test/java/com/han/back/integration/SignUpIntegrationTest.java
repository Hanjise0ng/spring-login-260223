package com.han.back.integration;

import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.security.token.LoginIdTokenUtil;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// [이메일 발송 처리 전략]
// 실제 SMTP 발송은 통합 테스트에서 의미 없음. JavaMailSender를 @MockitoBean으로 교체
// (@MockBean은 Spring Boot 3.4 / Spring Boot 4에서 @MockitoBean으로 대체됨)
//
// 이메일 발송 자체는 단위 테스트(EmailNotificationSender)에서 검증
// 인증 코드는 발송 직후 Redis에서 직접 읽어 사용 → 실제 플로우와 동일한 상태를 만듦
//
// [LoginIdTokenUtil 처리 전략]
// HMAC 서명 기반 실제 Bean 사용 → checkLoginId 응답 token을 signUp에 그대로 전달
@DisplayName("회원가입 통합 테스트")
class SignUpIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private LoginIdTokenUtil loginIdTokenUtil;

    private static final String LOGIN_ID = "newuser01";
    private static final String EMAIL = "newuser@test.com";
    private static final String PASSWORD = "Test1234!";
    private static final String NICKNAME = "신규유저";

    @BeforeEach
    void setUpMailSenderMock() {
        // Session 없이 빈 MimeMessage 객체 생성
        MimeMessage mimeMessage = new MimeMessage((Session) null);

        // createMimeMessage() 호출 시 빈 MimeMessage 반환
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // send() 호출 시 아무 동작도 하지 않음
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
    }

    private String checkLoginId(String loginId) throws Exception {
        ResultActions result = mockMvc.perform(get("/api/v1/auth/check-login-id")
                .param("loginId", loginId));
        return objectMapper.readTree(
                result.andReturn().getResponse().getContentAsString()
        ).path("result").path("loginIdToken").asString();
    }

    private void sendVerificationCode(String email) throws Exception {
        // JavaMailSender Mock → 실제 SMTP 발송 없이 Redis에 코드만 저장
        mockMvc.perform(post("/api/v1/verification/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "target", email, "type", "SIGN_UP", "channel", "EMAIL"
                ))));
    }

    // 발송 후 Redis에 저장된 실제 코드 직접 조회
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

    // 이메일 발송/확인 단계를 건너뛸 때 Redis에 confirmed키 직접 주입
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
        @DisplayName("4단계 플로우를 완료하면 사용자가 DB에 저장되고 Redis confirmed 키가 소비된다")
        void fullSignUpFlow_savesUserAndConsumesConfirmedKey() throws Exception {
            // 아이디 중복 확인
            String loginIdToken = checkLoginId(LOGIN_ID);
            assertThat(loginIdToken).isNotEmpty();

            // 인증 코드 발송
            sendVerificationCode(EMAIL);
            String code = getStoredVerificationCode(EMAIL);
            assertThat(code).hasSize(6);

            // Redis: code + cooldown 키 생성 확인
            assertThat(redisTemplate.hasKey(
                    VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL))).isTrue();
            assertThat(redisTemplate.hasKey(
                    VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL))).isTrue();

            // 인증 코드 확인
            confirmVerificationCode(EMAIL, code);

            // Redis: code 키 삭제 + confirmed 키 생성
            assertThat(redisTemplate.hasKey(
                    VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL))).isFalse();
            assertThat(redisTemplate.hasKey(
                    VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL))).isTrue();

            // 회원가입
            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SU"));

            // DB 저장 확인
            assertThat(userRepository.existsByLoginId(LOGIN_ID)).isTrue();
            assertThat(userRepository.existsByEmail(EMAIL)).isTrue();

            // Redis: confirmed 키 소비됨 (재가입 방지)
            assertThat(redisTemplate.hasKey(
                    VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL))).isFalse();
        }

        @Test
        @DisplayName("회원가입 완료 후 동일 이메일로 재가입 시도하면 VNC가 반환된다")
        void signUpTwice_withConsumedConfirmedKey_returnsVnc() throws Exception {
            // 첫 번째 가입
            String token1 = checkLoginId(LOGIN_ID);
            injectConfirmedKey(EMAIL);
            requestSignUp(LOGIN_ID, EMAIL, token1).andExpect(status().isOk());

            // 두 번째 시도 — confirmed 키가 소비됐으므로 VNC
            String token2 = checkLoginId("newuser02");
            requestSignUp("newuser02", EMAIL, token2)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VNC"));
        }
    }

    @Nested
    @DisplayName("checkLoginId 실패")
    class CheckLoginIdFailures {

        @Test
        @DisplayName("이미 가입된 loginId로 중복 확인 시 409 DI가 반환된다")
        void duplicateLoginId_returns409Di() throws Exception {
            signUpAs(LOGIN_ID, EMAIL);

            mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", LOGIN_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DI"));
        }

        @Test
        @DisplayName("loginId가 빈 값이면 400 VF가 반환된다")
        void emptyLoginId_returns400Vf() throws Exception {
            mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VF"));
        }
    }

    @Nested
    @DisplayName("인증 코드 발송 실패")
    class VerificationSendFailures {

        @Test
        @DisplayName("이미 가입된 이메일로 발송 시 409 DE가 반환된다")
        void duplicateEmail_returns409De() throws Exception {
            signUpAs("existinguser", EMAIL);

            mockMvc.perform(post("/api/v1/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "type", "SIGN_UP", "channel", "EMAIL"
                            ))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DE"));
        }

        @Test
        @DisplayName("60초 내 재발송 시 429 CA가 반환된다")
        void cooldownViolation_returns429Ca() throws Exception {
            sendVerificationCode(EMAIL);

            // 즉시 재발송 → 쿨다운 위반
            mockMvc.perform(post("/api/v1/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "type", "SIGN_UP", "channel", "EMAIL"
                            ))))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("CA"));
        }

        @Test
        @DisplayName("미지원 채널(SMS) 요청 시 422 UNC가 반환된다 — DB 조회 없이 즉시 차단")
        void unsupportedChannel_returns422Unc() throws Exception {
            mockMvc.perform(post("/api/v1/verification/send")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "type", "SIGN_UP", "channel", "SMS"
                            ))))
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("UNC"));
        }
    }

    @Nested
    @DisplayName("인증 코드 확인 실패")
    class VerificationConfirmFailures {

        @Test
        @DisplayName("틀린 코드 입력 시 400 CF가 반환되고 code 키는 유지된다")
        void wrongCode_returns400CfAndKeepsCodeKey() throws Exception {
            sendVerificationCode(EMAIL);
            String correctCode = getStoredVerificationCode(EMAIL);

            mockMvc.perform(post("/api/v1/verification/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "target", EMAIL, "code", "000000", "type", "SIGN_UP"
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("CF"));

            // 코드 키는 삭제되지 않아야 함 (재시도 가능)
            assertThat(getStoredVerificationCode(EMAIL)).isEqualTo(correctCode);
        }
    }

    @Nested
    @DisplayName("signUp 실패")
    class SignUpFailures {

        @Test
        @DisplayName("loginIdToken 필드 없이 회원가입 시 400 VF가 반환된다 — @NotBlank 검증")
        void missingLoginIdToken_returns400Vf() throws Exception {
            injectConfirmedKey(EMAIL);

            mockMvc.perform(post("/api/v1/auth/sign-up")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "loginId", LOGIN_ID, "password", PASSWORD,
                                    "email", EMAIL, "nickname", NICKNAME
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VF"));
        }

        @Test
        @DisplayName("위변조된 loginIdToken으로 회원가입 시 400 LICR이 반환된다")
        void tamperedLoginIdToken_returns400Licr() throws Exception {
            injectConfirmedKey(EMAIL);

            requestSignUp(LOGIN_ID, EMAIL, "aW52YWxpZC50b2tlbg")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("LICR"));
        }

        @Test
        @DisplayName("다른 loginId로 발급된 토큰을 사용하면 400 LICR이 반환된다")
        void mismatchedLoginIdToken_returns400Licr() throws Exception {
            // "otherid"로 발급된 토큰을 LOGIN_ID로 가입 시도 — LoginIdTokenUtil 실제 Bean 사용
            String otherToken = loginIdTokenUtil.issue("otherid");
            injectConfirmedKey(EMAIL);

            requestSignUp(LOGIN_ID, EMAIL, otherToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("LICR"));
        }

        @Test
        @DisplayName("이메일 인증 없이 회원가입 시 400 VNC가 반환되고 DB에 저장되지 않는다")
        void noEmailVerification_returns400VncAndNoDbEntry() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VNC"));

            assertThat(userRepository.existsByLoginId(LOGIN_ID)).isFalse();
        }

        @Test
        @DisplayName("시간차로 같은 아이디가 선점되면 409 DI가 반환된다 — belt-and-suspenders")
        void loginIdRaceCondition_returns409Di() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);
            injectConfirmedKey(EMAIL);
            // 시간차 방어: 동일 loginId로 다른 사용자가 먼저 가입
            signUpAs(LOGIN_ID, "other@test.com");

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DI"));
        }

        @Test
        @DisplayName("시간차로 같은 이메일이 선점되면 409 DE가 반환된다 — belt-and-suspenders")
        void emailRaceCondition_returns409De() throws Exception {
            String loginIdToken = checkLoginId(LOGIN_ID);
            injectConfirmedKey(EMAIL);
            signUpAs("otherid", EMAIL);

            requestSignUp(LOGIN_ID, EMAIL, loginIdToken)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DE"));
        }
    }

}