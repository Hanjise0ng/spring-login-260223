package com.han.back.integration;

import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.fixture.UserFixture;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.JwtUtil;
import com.han.back.global.util.SecurityPathConst;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("로그아웃 통합 테스트")
class LogoutIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JwtUtil jwtUtil;

    // JWT AT에서 sessionId claim 추출
    private String extractSessionId(String accessToken) {
        Claims claims = jwtUtil.parseClaims(accessToken);
        return jwtUtil.getSessionId(claims);
    }

    @Nested
    @DisplayName("정상 로그아웃")
    class SuccessfulLogout {

        @Test
        @DisplayName("유효한 AT로 로그아웃하면 Redis RT 삭제, 세션 블랙리스트 등록, 디바이스 비활성화가 된다")
        void logoutWithValidAt_cleansUpAllState() throws Exception {
            // Given
            UserEntity user = signUp();
            ResultActions login = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-web-001");

            String at = getAt(login);
            String sessionId = extractSessionId(at);

            ResultActions logout = mockMvc.perform(post(SecurityPathConst.LOGOUT_PATH)
                    .header("Authorization", "Bearer " + at));

            // HTTP + 쿠키 삭제 지시
            logout.andExpect(status().isOk())
                    .andExpect(cookie().maxAge(AuthConst.COOKIE_REFRESH_TOKEN_NAME, 0))
                    .andExpect(cookie().value(AuthConst.COOKIE_REFRESH_TOKEN_NAME, ""));

            // Redis: RT 삭제 + 세션 블랙리스트 등록
            assertThat(getRtKeys(user.getId())).isEmpty();
            assertThat(isBlacklisted(sessionId)).isTrue();

            // DB: DeviceEntity 세션 비활성화
            List<DeviceEntity> devices = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId());
            assertThat(devices).hasSize(1);
            assertThat(devices.getFirst().hasActiveSession()).isFalse();
            assertThat(devices.getFirst().getSessionId()).isNull();
        }

        @Test
        @DisplayName("AT 만료 시 RT 쿠키만으로 로그아웃하면 동일하게 세션이 무효화된다")
        void logoutWithRtFallback_cleansUpAllState() throws Exception {
            UserEntity user = signUp();
            ResultActions login = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-web-002");

            String at = getAt(login);
            String rt = getCookieValue(login, AuthConst.COOKIE_REFRESH_TOKEN_NAME);
            String sessionId = extractSessionId(at);

            // AT 없이 RT 쿠키만 전송 (AT 만료 상황 시뮬레이션)
            ResultActions logout = mockMvc.perform(post(SecurityPathConst.LOGOUT_PATH)
                    .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, rt)));

            logout.andExpect(status().isOk())
                    .andExpect(cookie().maxAge(AuthConst.COOKIE_REFRESH_TOKEN_NAME, 0));

            assertThat(getRtKeys(user.getId())).isEmpty();
            assertThat(isBlacklisted(sessionId)).isTrue();

            List<DeviceEntity> devices = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId());
            assertThat(devices.getFirst().hasActiveSession()).isFalse();
        }

        @Test
        @DisplayName("로그아웃 후 동일 AT로 API 호출 시 401 AUF가 반환된다")
        void logoutThenUseOldAt_returns401() throws Exception {
            UserEntity user = signUp();
            ResultActions login = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-web-003");
            String at = getAt(login);

            // 로그아웃
            mockMvc.perform(post(SecurityPathConst.LOGOUT_PATH)
                            .header("Authorization", "Bearer " + at))
                    .andExpect(status().isOk());

            // 로그아웃된 AT로 인증 필요 API 재호출
            ResultActions reuse = mockMvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .get("/api/v1/devices")
                            .header("Authorization", "Bearer " + at));

            // 세션 블랙리스트로 인해 인증 실패
            reuse.andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUF"));
        }

        @Test
        @DisplayName("로그아웃 후 동일 RT로 재발급 시도하면 401이 반환된다")
        void logoutThenReissueWithOldRt_returns401() throws Exception {
            UserEntity user = signUp();
            ResultActions login = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String at = getAt(login);
            String rt = getCookieValue(login, AuthConst.COOKIE_REFRESH_TOKEN_NAME);

            mockMvc.perform(post(SecurityPathConst.LOGOUT_PATH)
                            .header("Authorization", "Bearer " + at))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/v1/auth/reissue")
                            .header("Authorization", "Bearer " + at)
                            .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, rt)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("비정상 로그아웃")
    class FailedLogout {

        @Test
        @DisplayName("AT도 RT도 없이 로그아웃 시도하면 401을 반환한다")
        void logoutWithNoCredentials_returns401() throws Exception {
            mockMvc.perform(post(SecurityPathConst.LOGOUT_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("위조된 RT 쿠키만으로 로그아웃 시도하면 401을 반환한다")
        void logoutWithTamperedRt_returns401() throws Exception {
            mockMvc.perform(post(SecurityPathConst.LOGOUT_PATH)
                            .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, "tampered.rt.value")))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("다중 기기 독립성")
    class MultiDeviceIsolation {

        @Test
        @DisplayName("기기 A 로그아웃 시 기기 B의 Redis RT와 DB 세션은 유지된다")
        void logoutDeviceA_preservesDeviceBSession() throws Exception {
            // 기기 A, B 각각 로그인
            UserEntity user = signUp();

            ResultActions loginA = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-device-A");
            String atA = getAt(loginA);
            String sessionIdA = extractSessionId(atA);

            ResultActions loginB = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-device-B");
            String atB = getAt(loginB);
            String sessionIdB = extractSessionId(atB);

            assertThat(getRtKeys(user.getId())).hasSize(2);

            // 기기 A 로그아웃
            mockMvc.perform(post(SecurityPathConst.LOGOUT_PATH)
                            .header("Authorization", "Bearer " + atA))
                    .andExpect(status().isOk());

            // 기기 A 무효화
            assertThat(isBlacklisted(sessionIdA)).isTrue();
            assertThat(getRtKeys(user.getId())).hasSize(1);

            // 기기 B 유지
            assertThat(isBlacklisted(sessionIdB)).isFalse();
            assertThat(getRedisValue(
                    AuthConst.TOKEN_REFRESH_REDIS_PREFIX + user.getId() + ":" + sessionIdB))
                    .isNotNull();

            // DB: A 비활성, B 활성
            List<DeviceEntity> devices = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId());
            assertThat(devices).hasSize(2);

            assertThat(devices.stream()
                    .filter(d -> "fp-device-A".equals(d.getDeviceFingerprint()))
                    .findFirst().orElseThrow().hasActiveSession()).isFalse();

            assertThat(devices.stream()
                    .filter(d -> "fp-device-B".equals(d.getDeviceFingerprint()))
                    .findFirst().orElseThrow().hasActiveSession()).isTrue();
        }
    }

}