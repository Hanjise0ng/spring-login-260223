package com.han.back.integration;

import com.han.back.domain.user.entity.UserEntity;
import com.han.back.fixture.UserFixture;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 로그인 API 통합 테스트 — LoginFilter + TokenService 연동 검증
//
// 이 클래스의 책임 (DeviceIntegrationTest와의 경계):
//   - 로그인 성공 응답 형식 (헤더, 쿠키 값 검증)
//   - 발급된 AT JWT claims 내용 검증
//   - 로그인 실패 HTTP 응답 코드 검증
//   - 블랙리스트된 AT로 인증 필요 API 호출 시 거부 검증
//   - 토큰 재발급 (reissue) 플로우
//
// DeviceEntity 생성/갱신, 세션 무효화 세부 로직 → DeviceIntegrationTest 담당
@DisplayName("로그인 통합 테스트")
class LoginIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JwtUtil jwtUtil;

    private String extractSessionId(String at) {
        Claims claims = jwtUtil.parseClaims(at);
        return jwtUtil.getSessionId(claims);
    }

    @Nested
    @DisplayName("로그인 성공")
    class SuccessfulLogin {

        @Test
        @DisplayName("올바른 자격증명으로 로그인하면 AT 헤더, RT 쿠키, device_id 쿠키에 값이 담겨 응답된다")
        void validCredentials_returnsTokensInHeaderAndCookie() throws Exception {
            UserEntity user = signUp();

            ResultActions result = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);

            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SU"));

            assertThat(getAt(result)).isNotBlank();
            assertThat(getCookieValue(result, AuthConst.COOKIE_REFRESH_TOKEN_NAME)).isNotBlank();
            assertThat(getCookieValue(result, AuthConst.COOKIE_DEVICE_ID_NAME)).isNotBlank();
        }

        @Test
        @DisplayName("발급된 AT의 JWT claims에 userPk, role, sessionId, category가 올바르게 담긴다")
        void issuedAt_containsCorrectClaims() throws Exception {
            UserEntity user = signUp();

            String at = getAt(signIn(user.getLoginId(), UserFixture.RAW_PASSWORD));
            Claims claims = jwtUtil.parseClaims(at);

            assertThat(jwtUtil.getId(claims)).isEqualTo(user.getId());
            assertThat(jwtUtil.getRole(claims).name()).isEqualTo("USER");
            assertThat(jwtUtil.getSessionId(claims)).isNotBlank();
            assertThat(jwtUtil.getCategory(claims)).isEqualTo(AuthConst.TOKEN_TYPE_ACCESS);
        }
    }

    @Nested
    @DisplayName("로그인 실패")
    class FailedLogin {

        @Test
        @DisplayName("잘못된 비밀번호로 로그인하면 401 SF가 반환된다")
        void wrongPassword_returns401Sf() throws Exception {
            UserEntity user = signUp();

            signIn(user.getLoginId(), "WrongPass999!")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("SF"));
        }

        @Test
        @DisplayName("존재하지 않는 loginId로 로그인하면 401 SF가 반환된다")
        void nonExistentLoginId_returns401Sf() throws Exception {
            signIn("ghost_user", UserFixture.RAW_PASSWORD)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("SF"));
        }

        @Test
        @DisplayName("빈 바디로 로그인하면 401이 반환된다")
        void emptyBody_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 실패 시 Redis에 RT가 저장되지 않는다")
        void failedLogin_doesNotStoreRtInRedis() throws Exception {
            UserEntity user = signUp();

            signIn(user.getLoginId(), "WrongPass999!");

            // Redis RT 여부만 — DeviceEntity 생성 여부는 DeviceIntegrationTest 담당
            assertThat(getRtKeys(user.getId())).isEmpty();
        }
    }

    // =========================================================================
    // 블랙리스트 AT 재사용 차단 — HTTP 레벨 검증
    //
    // DeviceIntegrationTest: Redis·DB 상태 변경(블랙리스트 키 생성, RT 삭제) 검증
    // 이 테스트: 블랙리스트 등록 결과로 실제 API 호출이 거부되는지 검증
    // =========================================================================

    @Nested
    @DisplayName("블랙리스트 AT 재사용 차단")
    class BlacklistedAtRejection {

        @Test
        @DisplayName("재로그인 후 이전 AT로 인증 필요 API 호출 시 401 AUF가 반환된다")
        void reloginThenUseBlacklistedAt_returns401Auf() throws Exception {
            UserEntity user = signUp();

            ResultActions first = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String firstAt  = getAt(first);
            String firstRt  = getCookieValue(first, AuthConst.COOKIE_REFRESH_TOKEN_NAME);

            // 재로그인 → 이전 세션 블랙리스트 등록
            reSignIn(user.getLoginId(), UserFixture.RAW_PASSWORD, firstAt, firstRt);

            // /api/v1/devices는 USER 권한 필요 → JwtFilter가 AT 검증
            // 블랙리스트에 등록된 세션 → AUF 반환
            mockMvc.perform(get("/api/v1/devices")
                            .header("Authorization", "Bearer " + firstAt))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUF"));
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class TokenReissue {

        @Test
        @DisplayName("유효한 RT로 재발급하면 새 AT·RT가 발급되고 세션이 롤링된다")
        void validRt_reissuesTokensAndRotatesSession() throws Exception {
            UserEntity user = signUp();
            ResultActions login = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);

            String oldAt        = getAt(login);
            String oldSessionId = extractSessionId(oldAt);
            String rt           = getCookieValue(login, AuthConst.COOKIE_REFRESH_TOKEN_NAME);

            // reissue는 AT + RT 모두 필요
            // AuthHttpUtil.extractRequiredTokenPair() 가 AT(Authorization 헤더)와 RT(쿠키) 동시 요구
            // AT가 없으면 MAT, RT가 없으면 MRT 순서로 검증
            ResultActions reissue = mockMvc.perform(post("/api/v1/auth/reissue")
                    .header("Authorization", "Bearer " + oldAt)
                    .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, rt)));

            reissue.andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SU"));

            String newAt        = getAt(reissue);
            String newSessionId = extractSessionId(newAt);
            assertThat(newAt).isNotBlank().isNotEqualTo(oldAt);
            assertThat(newSessionId).isNotEqualTo(oldSessionId);

            assertThat(isBlacklisted(oldSessionId)).isTrue();
            assertThat(getRedisValue(
                    AuthConst.TOKEN_REFRESH_REDIS_PREFIX + user.getId() + ":" + oldSessionId))
                    .isNull();
            assertThat(getRtKeys(user.getId())).hasSize(1);
        }

        @Test
        @DisplayName("RT 없이 재발급 요청하면 401 MRT가 반환된다")
        void missingRt_returns401Mrt() throws Exception {
            // AT는 있지만 RT는 없는 상황 — MAT 이전에 MRT가 반환되려면 AT는 포함해야 함
            UserEntity user = signUp();
            ResultActions login = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String at = getAt(login);

            mockMvc.perform(post("/api/v1/auth/reissue")
                            .header("Authorization", "Bearer " + at))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("MRT"));
        }
    }

    @Test
    @DisplayName("재발급 후 이전 RT로 다시 재발급 시도하면 401 AUF가 반환된다")
    void usedRt_reissueAgain_returns401() throws Exception {
        UserEntity user = signUp();
        ResultActions login = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);

        String at = getAt(login);
        String rt = getCookieValue(login, AuthConst.COOKIE_REFRESH_TOKEN_NAME);

        // 1차 재발급 성공
        mockMvc.perform(post("/api/v1/auth/reissue")
                        .header("Authorization", "Bearer " + at)
                        .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, rt)))
                .andExpect(status().isOk());

        // 동일 RT로 2차 재발급 시도 → 거부
        mockMvc.perform(post("/api/v1/auth/reissue")
                        .header("Authorization", "Bearer " + at)
                        .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, rt)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUF"));
    }

}