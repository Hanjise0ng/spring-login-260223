package com.han.back.integration;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.fixture.UserFixture;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.util.SocialSignUpTokenUtil;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 소셜 가입 후속 API 통합 테스트 — SocialAuthController + Security 인가 경로 + 가입 토큰 쿠키 연동 검증
//
// 이 클래스의 책임 (AuthServiceImplTest와의 경계):
//   - 인가 경로 — 후속 엔드포인트가 인증 없이 컨트롤러까지 도달하는가 (permitAll 회귀 방지)
//   - 가입 토큰 쿠키 — 요청에서 읽히고, 성공 시에만 만료되는가
//   - 응답 변환 — Authenticated는 AT/RT 발급, LinkSuggested는 status 응답
//   - 요청 바디 바인딩 — @Valid 검증, DTO 역직렬화
//
// LinkSuggested 판정 조건, 연동 규칙 등 도메인 분기 → AuthServiceImplTest / CredentialLinkServiceImplTest 담당
//
// 구성:
//   1. 인가 경로     — /complete, /separate, /link
//   2. 가입 완료     — POST /api/v1/auth/oauth2/complete
//   3. 별도 계정 생성 — POST /api/v1/auth/oauth2/separate
//   4. 기존 계정 연동 — POST /api/v1/auth/oauth2/link
@DisplayName("소셜 가입 후속 API 통합 테스트")
class SocialSignUpIntegrationTest extends IntegrationTestBase {

    @Autowired private SocialSignUpTokenUtil socialSignUpTokenUtil;

    private static final String BASE_URI = "/api/v1/auth/oauth2";
    private static final String PROVIDER_ID = "kakao-9876543210";
    private static final String SOCIAL_NICKNAME = "소셜유저";
    private static final String NEW_EMAIL = "social@test.com";
    private static final String LOCAL_EMAIL = "test@test.com";   // UserFixture.localUser()의 이메일

    private String signUpToken;

    @BeforeEach
    void issueSignUpToken() {
        // 운영의 OAuth2LoginSuccessHandler와 동일한 입력(AuthProvider.getValue())으로 발급
        signUpToken = socialSignUpTokenUtil.issue(AuthProvider.KAKAO.getValue(), PROVIDER_ID, SOCIAL_NICKNAME);
    }

    private Cookie signUpTokenCookie(String token) {
        return new Cookie(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME, token);
    }

    private void markEmailVerified(String email) {
        redisTemplate.opsForValue().set(
                VerificationConst.confirmedKey(VerificationType.SIGN_UP, email), "CONFIRMED");
    }

    private ResultActions requestWithEmail(String path, String email, Cookie... cookies) throws Exception {
        var request = post(BASE_URI + path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email)));
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mockMvc.perform(request);
    }

    private ResultActions requestLink(String loginId, String password, Cookie... cookies) throws Exception {
        var request = post(BASE_URI + "/link")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("loginId", loginId, "password", password)));
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return mockMvc.perform(request);
    }

    private CredentialEntity findKakaoCredential() {
        return credentialRepository.findByProviderAndIdentifier(AuthProvider.KAKAO, PROVIDER_ID).orElseThrow();
    }

    @Nested
    @DisplayName("인가 경로")
    class AuthorizationPath {

        // AT가 없으면 JwtFilter는 그냥 통과시키고, permitAll이 누락된 경로는 인가 단계(AuthorizationFilter)에서
        // 거부되어 EntryPoint가 401 AUTH_AUTHENTICATION_FAIL로 응답한다.
        // 컨트롤러까지 도달해야만 SOCIAL_SIGNUP_TOKEN_INVALID가 나오므로, 이 코드로 permitAll 적용을 증명한다.
        @ParameterizedTest(name = "POST {0}")
        @ValueSource(strings = {"/complete", "/separate"})
        @DisplayName("AT 없이 이메일 기반 후속 요청 시 401이 아닌 400 SOCIAL_SIGNUP_TOKEN_INVALID가 반환된다")
        void emailFollowUpWithoutAt_reachesController(String path) throws Exception {
            requestWithEmail(path, NEW_EMAIL)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SOCIAL_SIGNUP_TOKEN_INVALID"));
        }

        @Test
        @DisplayName("AT 없이 연동 요청 시 401이 아닌 400 SOCIAL_SIGNUP_TOKEN_INVALID가 반환된다")
        void linkWithoutAt_reachesController() throws Exception {
            requestLink(UserFixture.DEFAULT_LOGIN_ID, UserFixture.RAW_PASSWORD)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SOCIAL_SIGNUP_TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("가입 완료")
    class CompleteSignUp {

        @Test
        @DisplayName("이메일 인증 완료 후 요청하면 AT 헤더·RT 쿠키가 발급되고 가입 토큰 쿠키가 만료된다")
        void complete_issuesTokensAndExpiresSignUpCookie() throws Exception {
            markEmailVerified(NEW_EMAIL);

            requestWithEmail("/complete", NEW_EMAIL, signUpTokenCookie(signUpToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
                    .andExpect(cookie().exists(AuthConst.COOKIE_REFRESH_TOKEN_NAME))
                    .andExpect(cookie().maxAge(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME, 0));
        }

        @Test
        @DisplayName("가입 토큰의 provider/providerId와 입력 이메일로 사용자와 소셜 credential이 생성된다")
        void complete_createsUserFromTokenClaimsAndInputEmail() throws Exception {
            markEmailVerified(NEW_EMAIL);

            requestWithEmail("/complete", NEW_EMAIL, signUpTokenCookie(signUpToken))
                    .andExpect(status().isOk());

            CredentialEntity credential = findKakaoCredential();
            UserEntity user = userRepository.findById(credential.getUserId()).orElseThrow();
            assertThat(user.getEmail()).isEqualTo(NEW_EMAIL);
            assertThat(user.getNickname()).isEqualTo(SOCIAL_NICKNAME);
        }

        @Test
        @DisplayName("LinkSuggested면 status=link_suggested로 응답하고, 다음 단계를 위해 가입 토큰 쿠키를 만료시키지 않는다")
        void complete_linkSuggested_keepsSignUpCookie() throws Exception {
            signUp();
            markEmailVerified(LOCAL_EMAIL);

            requestWithEmail("/complete", LOCAL_EMAIL, signUpTokenCookie(signUpToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.status").value(OAuth2Const.STATUS_LINK_SUGGESTED))
                    .andExpect(header().doesNotExist(HttpHeaders.AUTHORIZATION))
                    .andExpect(cookie().doesNotExist(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME));

            assertThat(credentialRepository.findByProviderAndIdentifier(AuthProvider.KAKAO, PROVIDER_ID)).isEmpty();
        }

        @Test
        @DisplayName("이메일 형식이 잘못되면 400 VALIDATION_FAIL이 반환되고 서비스까지 진입하지 않는다")
        void complete_invalidEmail_returns400() throws Exception {
            requestWithEmail("/complete", "not-an-email", signUpTokenCookie(signUpToken))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAIL"));
        }

        @Test
        @DisplayName("위조된 가입 토큰 쿠키로 요청하면 400 SOCIAL_SIGNUP_TOKEN_INVALID가 반환된다")
        void complete_forgedToken_returns400() throws Exception {
            markEmailVerified(NEW_EMAIL);

            requestWithEmail("/complete", NEW_EMAIL, signUpTokenCookie("forged.signup.token"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SOCIAL_SIGNUP_TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("별도 계정 생성")
    class SeparateAccount {

        @Test
        @DisplayName("LOCAL과 충돌하는 이메일이어도 별도 소셜 계정이 생성되고 가입 토큰 쿠키가 만료된다")
        void separate_conflictingEmail_createsNewAccount() throws Exception {
            UserEntity localUser = signUp();
            markEmailVerified(LOCAL_EMAIL);

            requestWithEmail("/separate", LOCAL_EMAIL, signUpTokenCookie(signUpToken))
                    .andExpect(status().isOk())
                    .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
                    .andExpect(cookie().maxAge(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME, 0));

            assertThat(findKakaoCredential().getUserId()).isNotEqualTo(localUser.getId());
            assertThat(userRepository.count()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("기존 계정 연동")
    class LinkToLocalAccount {

        @Test
        @DisplayName("본인 확인에 성공하면 기존 LOCAL 사용자에 소셜 credential이 붙고 가입 토큰 쿠키가 만료된다")
        void link_success_attachesCredentialToLocalUser() throws Exception {
            UserEntity localUser = signUp();

            requestLink(UserFixture.DEFAULT_LOGIN_ID, UserFixture.RAW_PASSWORD, signUpTokenCookie(signUpToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(cookie().maxAge(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME, 0));

            assertThat(findKakaoCredential().getUserId()).isEqualTo(localUser.getId());
            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401 AUTH_SIGN_IN_FAIL이 반환되고, 재시도를 위해 가입 토큰 쿠키를 유지한다")
        void link_wrongPassword_keepsSignUpCookie() throws Exception {
            signUp();

            requestLink(UserFixture.DEFAULT_LOGIN_ID, "Wrong1234!", signUpTokenCookie(signUpToken))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_SIGN_IN_FAIL"))
                    .andExpect(cookie().doesNotExist(OAuth2Const.COOKIE_SOCIAL_SIGNUP_TOKEN_NAME));

            assertThat(credentialRepository.findByProviderAndIdentifier(AuthProvider.KAKAO, PROVIDER_ID)).isEmpty();
        }
    }

}