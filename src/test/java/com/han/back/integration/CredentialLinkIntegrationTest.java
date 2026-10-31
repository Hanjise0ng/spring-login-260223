package com.han.back.integration;

import com.han.back.domain.auth.dto.SocialSignInResult;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.fixture.DeviceFixture;
import com.han.back.fixture.UserFixture;
import com.han.back.global.security.token.util.SocialLinkTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 소셜 연동 관리 API 통합 테스트 — CredentialLinkController + 인증 필터 + @AuthenticationPrincipal 연동 검증
//
// 이 클래스의 책임 (CredentialLinkServiceImplTest와의 경계):
//   - 인증 경계 — 모든 엔드포인트가 AT 없이 거부되는가
//   - 주체 바인딩 — AT의 사용자 기준으로만 조회·변경되는가 (타인 데이터 격리)
//   - 요청 바인딩 — PathVariable enum 변환, @Valid 검증
//   - 실제 영속화 — 서비스 결과가 DB와 후속 로그인에 반영되는가
//
// 소셜 단독 계정 해제 거부, 중복 연동 거부 등 연동 규칙 → CredentialLinkServiceImplTest 담당
//
// 구성:
//   1. 인증 경계     — 4개 엔드포인트 공통
//   2. 연동 목록 조회 — GET /api/v1/auth/credentials/socials
//   3. 연동 해제     — DELETE /api/v1/auth/credentials/socials/{provider}
//   4. 로컬 계정 승격 — POST /api/v1/auth/credentials/local
//   5. 소셜 연동 시작 — POST /api/v1/auth/credentials/socials/link/start
@DisplayName("소셜 연동 관리 API 통합 테스트")
class CredentialLinkIntegrationTest extends IntegrationTestBase {

    @Autowired private AuthService authService;
    @Autowired private SocialLinkTokenUtil socialLinkTokenUtil;

    private static final String BASE_URI = "/api/v1/auth/credentials";
    private static final String KAKAO_ID = "kakao-1111";
    private static final String SOCIAL_EMAIL = "social@test.com";
    private static final String PROMOTED_LOGIN_ID = "promoted01";

    private String signInAsLocalUser() throws Exception {
        return getAt(signIn(UserFixture.DEFAULT_LOGIN_ID, UserFixture.RAW_PASSWORD));
    }

    // 소셜 단독 사용자는 로그인 API가 없으므로, OAuth2 콜백이 호출하는 서비스 진입점으로 세션을 만든다
    private String signInAsSocialOnlyUser() {
        OAuth2UserInfo userInfo = new OAuth2UserInfo() {
            @Override public AuthProvider getProvider() { return AuthProvider.KAKAO; }
            @Override public String getProviderId() { return KAKAO_ID; }
            @Override public String getEmail() { return SOCIAL_EMAIL; }
            @Override public String getNickname() { return "소셜유저"; }
        };
        SocialSignInResult result = authService.processSocialLogin(userInfo, DeviceFixture.webDeviceInfo());
        return ((SocialSignInResult.Authenticated) result).getSignInResult().getTokens().getAccessToken();
    }

    private void linkSocial(Long userId, AuthProvider provider, String providerId) {
        credentialRepository.save(UserFixture.socialCredential(userId, provider, providerId));
    }

    private String issueLoginIdToken(String loginId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/auth/check-login-id").param("loginId", loginId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("result").path("loginIdToken").asString();
    }

    private ResultActions requestPromote(String accessToken, String loginId, String password, String loginIdToken)
            throws Exception {
        return mockMvc.perform(post(BASE_URI + "/local")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "loginId", loginId, "password", password, "loginIdToken", loginIdToken))));
    }

    @Nested
    @DisplayName("인증 경계")
    class AuthenticationBoundary {

        static Stream<Arguments> endpoints() {
            return Stream.of(
                    Arguments.of("GET /socials", get(BASE_URI + "/socials")),
                    Arguments.of("DELETE /socials/KAKAO", delete(BASE_URI + "/socials/KAKAO")),
                    Arguments.of("POST /local", post(BASE_URI + "/local")
                            .contentType(MediaType.APPLICATION_JSON).content("{}")),
                    Arguments.of("POST /socials/link/start", post(BASE_URI + "/socials/link/start")));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("endpoints")
        @DisplayName("AT 없이 요청하면 401 AUTH_AUTHENTICATION_FAIL이 반환된다")
        void withoutAt_returns401(String name, MockHttpServletRequestBuilder request) throws Exception {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTH_AUTHENTICATION_FAIL"));
        }
    }

    @Nested
    @DisplayName("연동 목록 조회")
    class GetLinkedSocials {

        @Test
        @DisplayName("AT 사용자의 소셜 연동만 반환하고 다른 사용자의 연동은 포함하지 않는다")
        void getLinkedSocials_returnsOnlyPrincipalCredentials() throws Exception {
            UserEntity me = signUp();
            linkSocial(me.getId(), AuthProvider.KAKAO, KAKAO_ID);
            UserEntity other = signUpAs("other01", "other@test.com");
            linkSocial(other.getId(), AuthProvider.NAVER, "naver-2222");

            mockMvc.perform(get(BASE_URI + "/socials")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + signInAsLocalUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.length()").value(1))
                    .andExpect(jsonPath("$.result[0].provider").value("KAKAO"));
        }
    }

    @Nested
    @DisplayName("연동 해제")
    class UnlinkSocial {

        @Test
        @DisplayName("연동된 소셜을 해제하면 DB에서 해당 credential이 삭제되고 LOCAL은 유지된다")
        void unlink_deletesSocialCredential() throws Exception {
            UserEntity me = signUp();
            linkSocial(me.getId(), AuthProvider.KAKAO, KAKAO_ID);

            mockMvc.perform(delete(BASE_URI + "/socials/KAKAO")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + signInAsLocalUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            assertThat(credentialRepository.findByProviderAndIdentifier(AuthProvider.KAKAO, KAKAO_ID)).isEmpty();
            assertThat(credentialRepository.findByUserIdAndProvider(me.getId(), AuthProvider.LOCAL)).isPresent();
        }

        @Test
        @DisplayName("지원하지 않는 provider 경로값이면 400 VALIDATION_FAIL이 반환된다")
        void unlink_unknownProvider_returns400() throws Exception {
            signUp();

            mockMvc.perform(delete(BASE_URI + "/socials/GITHUB")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + signInAsLocalUser()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAIL"));
        }
    }

    @Nested
    @DisplayName("로컬 계정 승격")
    class PromoteToLocal {

        @Test
        @DisplayName("소셜 단독 사용자가 승격하면 같은 사용자에 LOCAL이 추가되고 새 아이디로 로그인할 수 있다")
        void promote_thenSignInWithNewLoginId() throws Exception {
            String socialAt = signInAsSocialOnlyUser();
            Long socialUserId = credentialRepository
                    .findByProviderAndIdentifier(AuthProvider.KAKAO, KAKAO_ID).orElseThrow().getUserId();

            requestPromote(socialAt, PROMOTED_LOGIN_ID, UserFixture.RAW_PASSWORD, issueLoginIdToken(PROMOTED_LOGIN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));

            assertThat(credentialRepository.findByUserIdAndProvider(socialUserId, AuthProvider.LOCAL)).isPresent();
            signIn(PROMOTED_LOGIN_ID, UserFixture.RAW_PASSWORD)
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("비밀번호 정책 위반이면 400 VALIDATION_FAIL이 반환되고 LOCAL이 생성되지 않는다")
        void promote_weakPassword_returns400() throws Exception {
            String socialAt = signInAsSocialOnlyUser();

            requestPromote(socialAt, PROMOTED_LOGIN_ID, "short", issueLoginIdToken(PROMOTED_LOGIN_ID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAIL"));

            assertThat(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, PROMOTED_LOGIN_ID))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("소셜 연동 시작")
    class StartSocialLink {

        @Test
        @DisplayName("발급된 연동 토큰에는 AT 사용자의 userId가 담긴다")
        void startSocialLink_issuesTokenBoundToPrincipal() throws Exception {
            UserEntity me = signUp();

            String body = mockMvc.perform(post(BASE_URI + "/socials/link/start")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + signInAsLocalUser()))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            String linkToken = objectMapper.readTree(body).path("result").path("linkToken").asString();

            assertThat(socialLinkTokenUtil.validate(linkToken)).isEqualTo(me.getId());
        }
    }

}