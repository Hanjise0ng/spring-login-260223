package com.han.back.integration;

import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.fixture.UserFixture;
import com.han.back.global.security.util.AuthConst;
import com.han.back.global.security.util.JwtUtil;
import io.jsonwebtoken.Claims;
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

@DisplayName("LoginDeviceFlowTest — 로그인 통합 테스트")
class LoginDeviceFlowTest extends IntegrationTestBase {

    @Autowired
    private JwtUtil jwtUtil;

    private String extractSessionId(String accessToken) {
        Claims claims = jwtUtil.parseClaims(accessToken);
        return jwtUtil.getSessionId(claims);
    }

    @Nested
    @DisplayName("정상 로그인")
    class SuccessfulLogin {

        @Test
        @DisplayName("로그인 성공 시 AT 헤더, RT 쿠키, device_id 쿠키가 응답에 포함된다")
        void login_returnsTokensAndCookies() throws Exception {
            UserEntity userA = signUp();

            signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD)
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Authorization"))
                    .andExpect(cookie().exists(AuthConst.COOKIE_REFRESH_TOKEN_NAME))
                    .andExpect(cookie().exists(AuthConst.COOKIE_DEVICE_ID_NAME))
                    .andExpect(cookie().httpOnly(AuthConst.COOKIE_REFRESH_TOKEN_NAME, true))
                    .andExpect(cookie().httpOnly(AuthConst.COOKIE_DEVICE_ID_NAME, true));
        }

        @Test
        @DisplayName("로그인 성공 시 DeviceEntity가 DB에 1개 생성된다")
        void login_createsDeviceEntity() throws Exception {
            UserEntity userA = signUp();

            signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);

            List<DeviceEntity> devices = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(userA.getId());

            assertThat(devices).hasSize(1);
            assertThat(devices.getFirst().getSessionId()).isNotNull();
            assertThat(devices.getFirst().getOsName()).isNotBlank();
            assertThat(devices.getFirst().getBrowserName()).isNotBlank();
        }

        @Test
        @DisplayName("로그인 성공 시 Redis에 RT가 저장된다")
        void login_storesRtInRedis() throws Exception {
            UserEntity userA = signUp();

            signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);

            assertThat(getRtKeys(userA.getId())).hasSize(1);
        }

        @Test
        @DisplayName("AT의 sessionId와 DeviceEntity의 sessionId가 일치한다")
        void login_atSessionId_matchesDeviceSessionId() throws Exception {
            UserEntity userA = signUp();

            ResultActions result = signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);

            String atSessionId = extractSessionId(getAt(result));
            DeviceEntity device = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(userA.getId()).getFirst();

            assertThat(atSessionId).isEqualTo(device.getSessionId());
        }

        @Test
        @DisplayName("AT의 sessionId와 Redis RT키의 sessionId가 일치한다")
        void login_atSessionId_matchesRedisRtKey() throws Exception {
            UserEntity userA = signUp();

            ResultActions result = signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);

            String at = getAt(result);
            String atSessionId = extractSessionId(at);

            String expectedKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userA.getId() + ":" + atSessionId;
            assertThat(getRedisValue(expectedKey)).isNotNull();
        }
    }

    @Nested
    @DisplayName("로그인 실패")
    class FailedLogin {

        @Test
        @DisplayName("존재하지 않는 loginId → 401 반환")
        void nonExistentLoginId_returns401() throws Exception {
            signIn("wrong-user", UserFixture.RAW_PASSWORD)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비밀번호 불일치 → 401 반환")
        void wrongPassword_returns401() throws Exception {
            UserEntity userA = signUp();

            signIn(userA.getLoginId(), "WrongPass123!")
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("요청 바디가 없으면 → 401 반환")
        void emptyBody_returns401() throws Exception {
            mockMvc.perform(post("/api/v1/auth/sign-in")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("로그인 실패 시 DeviceEntity, Redis에 데이터가 생성되지 않는다")
        void failedLogin_createsNoData() throws Exception {
            UserEntity userA = signUp();

            signIn(userA.getLoginId(), "WrongPass123!");

            assertThat(deviceRepository.count()).isZero();
            assertThat(getRedisKeys("refresh:*")).isEmpty();
        }
    }

    @Nested
    @DisplayName("같은 기기 재로그인")
    class ReloginSameDevice {

        @Test
        @DisplayName("같은 device_id 쿠키로 재로그인 시 DeviceEntity가 새로 생성되지 않는다")
        void reloginSameDevice_doesNotCreateNewEntity() throws Exception {
            UserEntity userA = signUp();

            ResultActions first = signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, deviceId);

            assertThat(deviceRepository.count()).isEqualTo(1L);
        }

        @Test
        @DisplayName("재로그인 시 sessionId가 새로 발급된다")
        void reloginSameDevice_issuesNewSessionId() throws Exception {
            UserEntity userA = signUp();

            ResultActions first = signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);
            String firstSessionId = extractSessionId(getAt(first));
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            ResultActions second = signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, deviceId);
            String secondSessionId = extractSessionId(getAt(second));

            assertThat(secondSessionId).isNotEqualTo(firstSessionId);
        }

        @Test
        @DisplayName("AT/RT 없이 재로그인하면 이전 RT가 Redis에 그대로 남는다")
        void reloginWithoutTokens_oldRtRemainsInRedis() throws Exception {
            UserEntity userA = signUp();

            ResultActions first = signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            // AT/RT 없이 재로그인 → invalidatePreviousSessionIfPresent 미발동
            signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, deviceId);

            // 이전 RT + 새 RT → 2개
            assertThat(getRtKeys(userA.getId())).hasSize(2);
        }
    }

    @Nested
    @DisplayName("최대 세션 정책")
    class MaxSessionPolicy {

        @Test
        @DisplayName("세 번째 기기 로그인 시 가장 오래된 세션이 퇴출된다")
        void thirdLogin_evictsOldestSession() throws Exception {
            UserEntity userA = signUp();

            ResultActions loginA = signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fingerprint-a");
            String sessionA = extractSessionId(getAt(loginA));

            signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fingerprint-b");
            signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fingerprint-c");

            // 기기 A의 RT가 Redis에서 삭제됨
            String evictedRtKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userA.getId() + ":" + sessionA;
            assertThat(getRedisValue(evictedRtKey)).isNull();

            // 기기 A의 세션이 블랙리스트에 등록됨
            assertThat(isBlacklisted(sessionA)).isTrue();

            // 활성 DeviceEntity는 2개 (B, C)
            List<DeviceEntity> allDevices = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(userA.getId());
            long activeCount = allDevices.stream()
                    .filter(DeviceEntity::hasActiveSession)
                    .count();
            assertThat(activeCount).isEqualTo(2L);

            // 기기 A는 비활성 상태
            DeviceEntity deviceA = allDevices.stream()
                    .filter(d -> "fingerprint-a".equals(d.getDeviceFingerprint()))
                    .findFirst()
                    .orElseThrow();
            assertThat(deviceA.hasActiveSession()).isFalse();
        }
    }

    @Nested
    @DisplayName("이전 세션 무효화")
    class PreviousSessionInvalidation {

        @Test
        @DisplayName("AT + RT를 헤더에 담아 재로그인하면 이전 세션이 블랙리스트에 등록된다")
        void reloginWithTokens_blacklistsPreviousSession() throws Exception {
            UserEntity userA = signUp();

            // 1차 로그인
            ResultActions first = signIn(userA.getLoginId(), UserFixture.RAW_PASSWORD);
            String firstAt = getAt(first);
            String firstRt = getCookieValue(first, AuthConst.COOKIE_REFRESH_TOKEN_NAME);
            String firstSessionId = extractSessionId(firstAt);

            // AT + RT 포함해서 재로그인 (invalidatePreviousSessionIfPresent 발동)
            reSignIn(userA.getLoginId(), UserFixture.RAW_PASSWORD, firstAt, firstRt);

            // 이전 세션 블랙리스트 등록
            assertThat(isBlacklisted(firstSessionId)).isTrue();

            // 이전 RT Redis 삭제
            String oldRtKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userA.getId() + ":" + firstSessionId;
            assertThat(getRedisValue(oldRtKey)).isNull();
        }

        @Test
        @DisplayName("AT + RT + device_id 포함 재로그인하면 이전 RT 삭제되고 새 RT 1개만 남는다")
        void reloginWithTokensAndDevice_replacesRtInRedis() throws Exception {
            UserEntity userA = signUp();

            ResultActions first = signIn(
                    userA.getLoginId(), UserFixture.RAW_PASSWORD);
            String firstAt = getAt(first);
            String firstRt = getCookieValue(first, AuthConst.COOKIE_REFRESH_TOKEN_NAME);
            String firstSessionId = extractSessionId(firstAt);
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            reSignInWithDevice(
                    userA.getLoginId(), UserFixture.RAW_PASSWORD,
                    firstAt, firstRt, deviceId);

            String oldRtKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX
                    + userA.getId() + ":" + firstSessionId;
            assertThat(getRedisValue(oldRtKey)).isNull();
            assertThat(getRtKeys(userA.getId())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("같은 기기 타 유저 로그인")
    class SharedDeviceDifferentUser {

        @Test
        @DisplayName("유저A 로그인 후 유저B 가 같은 기기로 로그인해도 유저A 세션은 유지된다")
        void userBLogin_doesNotAffectUserASession() throws Exception {
            UserEntity userA = signUp();
            signUpAs("userB", "userB@test.com");

            ResultActions loginA = signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fingerprint-shared");
            String sessionA = extractSessionId(getAt(loginA));

            signInWithDevice("userB", UserFixture.RAW_PASSWORD, "fingerprint-shared");

            // 유저A RT 유지
            String userARtKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userA.getId() + ":" + sessionA;
            assertThat(getRedisValue(userARtKey)).isNotNull();

            // 유저A 세션 블랙리스트 미등록
            assertThat(isBlacklisted(sessionA)).isFalse();

            // DeviceEntity 유저A 1개, 유저B 1개 독립 생성
            assertThat(deviceRepository.count()).isEqualTo(2L);
        }

        @Test
        @DisplayName("같은 기기에서 A → B → A 순서 로그인 시 유저B 이전 세션이 무효화된다")
        void loginOrder_ABA_invalidatesUserBPreviousSession() throws Exception {
            UserEntity userA = signUp();
            UserEntity userB = signUpAs("userB", "userB@test.com");

            signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fingerprint-shared");

            ResultActions loginB = signInWithDevice("userB", UserFixture.RAW_PASSWORD, "fingerprint-shared");
            String sessionB = extractSessionId(getAt(loginB));

            // 유저A 재로그인 — B의 AT/RT 없이, fingerprint 동일
            signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fingerprint-shared");

            // 유저B 세션 영향받지 않음
            assertThat(isBlacklisted(sessionB)).isFalse();
            String userBRtKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userB.getId() + ":" + sessionB;
            assertThat(getRedisValue(userBRtKey)).isNotNull();

            // 유저A DeviceEntity 재활성화
            List<DeviceEntity> userADevices = deviceRepository
                    .findActiveDevicesByUserIdOldestFirst(
                            userRepository.findByLoginId(userA.getLoginId())
                                    .orElseThrow().getId());

            assertThat(userADevices).hasSize(1);
            assertThat(userADevices.getFirst().hasActiveSession()).isTrue();
        }

        @Test
        @DisplayName("유저B가 자신의 이전 세션이 있는 상태에서 재로그인하면 B의 이전 세션만 무효화된다")
        void userBRelogin_invalidatesOnlyUserBPreviousSession() throws Exception {
            UserEntity userA = signUp();
            signUpAs("userB", "userB@test.com");

            ResultActions loginA = signInWithDevice(
                    userA.getLoginId(), UserFixture.RAW_PASSWORD, "fingerprint-shared");
            String sessionA = extractSessionId(getAt(loginA));

            ResultActions loginB1 = signInWithDevice(
                    "userB", UserFixture.RAW_PASSWORD, "fingerprint-b");
            String sessionB1 = extractSessionId(getAt(loginB1));
            String rtB1 = getCookieValue(loginB1, AuthConst.COOKIE_REFRESH_TOKEN_NAME);

            reSignIn("userB", UserFixture.RAW_PASSWORD, getAt(loginB1), rtB1);

            // 유저B 이전 세션 무효화
            assertThat(isBlacklisted(sessionB1)).isTrue();

            // 유저A 세션 영향 없음
            assertThat(isBlacklisted(sessionA)).isFalse();
            String userARtKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userA.getId() + ":" + sessionA;
            assertThat(getRedisValue(userARtKey)).isNotNull();
        }
    }

}