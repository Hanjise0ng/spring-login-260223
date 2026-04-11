package com.han.back.integration;

import com.han.back.domain.device.entity.DeviceConst;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 디바이스 통합 테스트 — DeviceService + DeviceRepository + TokenService 연동 검증
//
// 구성:
//   1. 디바이스 등록     — 로그인 시 DeviceEntity 생성, sessionId 일관성
//   2. 재로그인 처리     — 같은 기기 재사용, sessionId 갱신
//   3. 이전 세션 무효화  — AT+RT 재로그인 시 이전 세션 블랙리스트
//   4. 최대 세션 정책    — MAX_SESSIONS_PER_USER 초과 시 가장 오래된 세션 퇴출
//   5. 공유 기기 격리    — 다른 사용자 간 세션 독립성
//   6. 디바이스 목록 API — GET /api/v1/devices
//   7. 강제 로그아웃 API — POST /api/v1/devices/{publicId}/logout
//   8. 디바이스 삭제 API — DELETE /api/v1/devices/{publicId}
@DisplayName("디바이스 통합 테스트")
class DeviceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private JwtUtil jwtUtil;

    // 존재하지 않는 디바이스 publicId — 실제 UUID 형식
    private static final String NON_EXISTENT_PUBLIC_ID = "00000000-0000-0000-0000-000000000000";

    private String extractSessionId(String at) {
        Claims claims = jwtUtil.parseClaims(at);
        return jwtUtil.getSessionId(claims);
    }

    // 디바이스 목록 API 호출 후 파싱
    private List<Map<String, Object>> getDeviceList(String at) throws Exception {
        String body = mockMvc.perform(get("/api/v1/devices")
                        .header("Authorization", "Bearer " + at))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(
                objectMapper.readTree(body).path("result").toString(),
                new TypeReference<List<Map<String, Object>>>() {
                }
        );
    }

    private String getOtherDevicePublicId(String at) throws Exception {
        return getDeviceList(at).stream()
                .filter(d -> Boolean.FALSE.equals(d.get("currentDevice")))
                .findFirst()
                .map(d -> (String) d.get("publicId"))
                .orElseThrow(() -> new IllegalStateException("다른 기기가 없음 — 2개 이상 로그인 필요"));
    }

    @Nested
    @DisplayName("디바이스 등록")
    class DeviceRegistration {

        @Test
        @DisplayName("로그인 성공 시 DeviceEntity가 DB에 1개 생성되고 sessionId, osName, browserName이 채워진다")
        void login_createsDeviceEntity() throws Exception {
            UserEntity user = signUp();
            signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);

            List<DeviceEntity> devices = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId());
            assertThat(devices).hasSize(1);
            assertThat(devices.getFirst().getSessionId()).isNotNull();
            assertThat(devices.getFirst().getOsName()).isNotBlank();
            assertThat(devices.getFirst().getBrowserName()).isNotBlank();
        }

        @Test
        @DisplayName("AT의 sessionId와 DeviceEntity의 sessionId가 일치한다")
        void login_atSessionIdMatchesDeviceSessionId() throws Exception {
            UserEntity user = signUp();

            ResultActions result = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String atSessionId = extractSessionId(getAt(result));

            DeviceEntity device = deviceRepository
                    .findAllByUserIdOrderByLastLoginAtDesc(user.getId()).getFirst();
            assertThat(atSessionId).isEqualTo(device.getSessionId());
        }

        @Test
        @DisplayName("AT의 sessionId와 Redis RT 키의 sessionId가 일치한다")
        void login_atSessionIdMatchesRedisRtKey() throws Exception {
            UserEntity user = signUp();

            ResultActions result = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String atSessionId = extractSessionId(getAt(result));

            String expectedKey = AuthConst.TOKEN_REFRESH_REDIS_PREFIX + user.getId() + ":" + atSessionId;
            assertThat(getRedisValue(expectedKey)).isNotNull();
        }

        @Test
        @DisplayName("로그인 실패 시 DeviceEntity와 Redis RT가 생성되지 않는다")
        void failedLogin_createsNoDeviceEntityOrRt() throws Exception {
            UserEntity user = signUp();
            signIn(user.getLoginId(), "WrongPass123!");

            assertThat(deviceRepository.count()).isZero();
            assertThat(getRedisKeys("refresh:*")).isEmpty();
        }
    }

    @Nested
    @DisplayName("재로그인 처리")
    class ReloginSameDevice {

        @Test
        @DisplayName("같은 device_id 쿠키로 재로그인 시 DeviceEntity가 새로 생성되지 않는다")
        void reloginSameDevice_doesNotCreateNewEntity() throws Exception {
            UserEntity user = signUp();

            ResultActions first = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, deviceId);

            assertThat(deviceRepository.count()).isEqualTo(1L);
        }

        @Test
        @DisplayName("재로그인 시 sessionId가 새로 발급된다")
        void reloginSameDevice_issuesNewSessionId() throws Exception {
            UserEntity user = signUp();

            ResultActions first = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String firstSessionId = extractSessionId(getAt(first));
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            ResultActions second = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, deviceId);
            String secondSessionId = extractSessionId(getAt(second));

            assertThat(secondSessionId).isNotEqualTo(firstSessionId);
        }

        @Test
        @DisplayName("AT/RT 없이 재로그인하면 이전 RT가 Redis에 남아 RT가 2개가 된다")
        void reloginWithoutTokens_oldRtRemainsInRedis() throws Exception {
            UserEntity user = signUp();

            ResultActions first = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            // AT/RT 없이 재로그인 → invalidatePreviousSessionIfPresent 미발동
            // 같은 DeviceEntity 재사용 → 활성 세션 수 DB 기준 1개 → MAX 정책 미발동
            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, deviceId);

            // 이전 세션의 RT는 Redis에 그대로 남아있어야 함
            assertThat(getRtKeys(user.getId())).hasSize(2);
        }
    }

    @Nested
    @DisplayName("이전 세션 무효화")
    class PreviousSessionInvalidation {

        @Test
        @DisplayName("AT + RT를 전송하며 재로그인하면 이전 세션이 블랙리스트에 등록되고 RT가 삭제된다")
        void reloginWithTokens_blacklistsPreviousSession() throws Exception {
            UserEntity user = signUp();

            ResultActions first = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String firstAt = getAt(first);
            String firstRt = getCookieValue(first, AuthConst.COOKIE_REFRESH_TOKEN_NAME);
            String firstSessionId = extractSessionId(firstAt);

            reSignIn(user.getLoginId(), UserFixture.RAW_PASSWORD, firstAt, firstRt);

            assertThat(isBlacklisted(firstSessionId)).isTrue();
            assertThat(getRedisValue(
                    AuthConst.TOKEN_REFRESH_REDIS_PREFIX + user.getId() + ":" + firstSessionId))
                    .isNull();
        }

        @Test
        @DisplayName("AT + RT + device_id 포함 재로그인 후 Redis에 새 RT 1개만 남는다")
        void reloginWithDevice_replacesRtInRedis() throws Exception {
            UserEntity user = signUp();

            ResultActions first = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);
            String firstAt = getAt(first);
            String firstRt = getCookieValue(first, AuthConst.COOKIE_REFRESH_TOKEN_NAME);
            String deviceId = getCookieValue(first, AuthConst.COOKIE_DEVICE_ID_NAME);

            reSignInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, firstAt, firstRt, deviceId);

            assertThat(getRtKeys(user.getId())).hasSize(1);
        }
    }

    @Nested
    @DisplayName("최대 세션 정책")
    class MaxSessionPolicy {

        @Test
        @DisplayName("(MAX+1)번째 기기 로그인 시 가장 오래된 세션이 Redis에서 삭제되고 블랙리스트에 등록된다")
        void maxPlusOneLogin_evictsOldestSessionFromRedis() throws Exception {
            UserEntity user = signUp();
            int max = DeviceConst.MAX_SESSIONS_PER_USER;

            // 1번째 로그인 — 가장 오래된 세션
            ResultActions first = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-1");
            String oldestSessionId = extractSessionId(getAt(first));

            // 2 ~ MAX번째까지 로그인
            for (int i = 2; i <= max; i++) {
                signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-" + i);
            }
            // MAX개 세션 — 아직 퇴출 없음 (size == MAX, 퇴출 조건: size > MAX)
            assertThat(getRtKeys(user.getId())).hasSize(max);

            // (MAX+1)번째 로그인 → 가장 오래된 세션(fp-1) 자동 퇴출
            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-" + (max + 1));

            // 퇴출 후에도 여전히 MAX개 세션
            assertThat(getRtKeys(user.getId())).hasSize(max);

            // 가장 오래된 세션의 RT 삭제 + 블랙리스트 등록
            assertThat(getRedisValue(
                    AuthConst.TOKEN_REFRESH_REDIS_PREFIX + user.getId() + ":" + oldestSessionId))
                    .isNull();
            assertThat(isBlacklisted(oldestSessionId)).isTrue();
        }

        @Test
        @DisplayName("(MAX+1)번째 기기 로그인 시 가장 오래된 DeviceEntity의 세션이 DB에서 비활성화된다")
        void maxPlusOneLogin_deactivatesOldestDeviceInDb() throws Exception {
            UserEntity user = signUp();
            int max = DeviceConst.MAX_SESSIONS_PER_USER;

            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-1");
            for (int i = 2; i <= max; i++) {
                signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-" + i);
            }
            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-" + (max + 1));

            DeviceEntity oldest = deviceRepository
                    .findAllByUserIdOrderByLastLoginAtDesc(user.getId()).stream()
                    .filter(d -> "fp-1".equals(d.getDeviceFingerprint()))
                    .findFirst().orElseThrow();

            assertThat(oldest.hasActiveSession()).isFalse();
            assertThat(oldest.getSessionId()).isNull();
        }
    }

    @Nested
    @DisplayName("공유 기기 격리")
    class SharedDeviceIsolation {

        @Test
        @DisplayName("유저A 로그인 후 유저B가 같은 기기로 로그인해도 유저A 세션은 유지된다")
        void userBLogin_doesNotAffectUserASession() throws Exception {
            UserEntity userA = signUp();
            signUpAs("userB", "userB@test.com");

            ResultActions loginA = signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fp-shared");
            String sessionA = extractSessionId(getAt(loginA));

            signInWithDevice("userB", UserFixture.RAW_PASSWORD, "fp-shared");

            assertThat(getRedisValue(
                    AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userA.getId() + ":" + sessionA))
                    .isNotNull();
            assertThat(isBlacklisted(sessionA)).isFalse();
            assertThat(deviceRepository.count()).isEqualTo(2L);
        }

        @Test
        @DisplayName("유저B가 이전 세션이 있는 상태에서 재로그인하면 유저B의 이전 세션만 무효화된다")
        void userBRelogin_invalidatesOnlyUserBSession() throws Exception {
            UserEntity userA = signUp();
            signUpAs("userB", "userB@test.com");

            ResultActions loginA = signInWithDevice(userA.getLoginId(), UserFixture.RAW_PASSWORD, "fp-shared");
            String sessionA = extractSessionId(getAt(loginA));

            ResultActions loginB1 = signInWithDevice("userB", UserFixture.RAW_PASSWORD, "fp-b");
            String sessionB1 = extractSessionId(getAt(loginB1));
            String rtB1 = getCookieValue(loginB1, AuthConst.COOKIE_REFRESH_TOKEN_NAME);

            reSignIn("userB", UserFixture.RAW_PASSWORD, getAt(loginB1), rtB1);

            assertThat(isBlacklisted(sessionB1)).isTrue();
            assertThat(isBlacklisted(sessionA)).isFalse();
            assertThat(getRedisValue(
                    AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userA.getId() + ":" + sessionA))
                    .isNotNull();
        }
    }

    @Nested
    @DisplayName("디바이스 목록 조회")
    class DeviceList {

        @Test
        @DisplayName("로그인 후 목록 조회 시 currentDevice=true인 항목이 정확히 1개이다")
        void getDevices_currentDeviceIsExactlyOne() throws Exception {
            UserEntity user = signUp();

            // 2개 기기 로그인 후 두 번째 AT로 목록 조회
            // MAX=2이므로 3번 로그인하면 퇴출 발생 — 2번으로 충분
            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-1");
            ResultActions second = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-2");
            String currentAt = getAt(second);

            List<Map<String, Object>> devices = getDeviceList(currentAt);

            long currentCount = devices.stream()
                    .filter(d -> Boolean.TRUE.equals(d.get("currentDevice")))
                    .count();
            assertThat(currentCount).isEqualTo(1);
        }

        @Test
        //   AT 없이 요청 시 동작 분석:
        //   JwtFilter → AT 없음 → SecurityContext 미설정 → 통과
        //   AnonymousAuthenticationFilter → anonymous 인증 설정
        //   AuthorizationFilter → anonymous에게 USER 권한 없음
        //   → CustomAuthenticationEntryPoint → 401 AUF
        @DisplayName("AT 없이 목록 조회 시 401 AUF가 반환된다")
        void getDevicesWithoutAt_returns401Auf() throws Exception {
            mockMvc.perform(get("/api/v1/devices"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUF"));
        }
    }

    @Nested
    @DisplayName("강제 로그아웃")
    class ForceLogout {

        @Test
        @DisplayName("다른 기기를 강제 로그아웃하면 해당 기기의 RT가 삭제되고 세션이 블랙리스트에 등록된다")
        void forceLogout_invalidatesTargetDeviceSession() throws Exception {
            UserEntity user = signUp();

            ResultActions loginA = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-A");
            String atA = getAt(loginA);
            String sessionA = extractSessionId(atA);

            ResultActions loginB = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-B");
            String atB = getAt(loginB);

            String publicIdA = getOtherDevicePublicId(atB);

            mockMvc.perform(post("/api/v1/devices/{publicId}/logout", publicIdA)
                            .header("Authorization", "Bearer " + atB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SU"));

            assertThat(isBlacklisted(sessionA)).isTrue();
            assertThat(getRedisValue(
                    AuthConst.TOKEN_REFRESH_REDIS_PREFIX + user.getId() + ":" + sessionA))
                    .isNull();

            DeviceEntity deviceA = deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId())
                    .stream()
                    .filter(d -> "fp-A".equals(d.getDeviceFingerprint()))
                    .findFirst().orElseThrow();
            assertThat(deviceA.hasActiveSession()).isFalse();
        }

        @Test
        @DisplayName("현재 기기를 강제 로그아웃 시도하면 400 SDL이 반환된다")
        void forceLogoutCurrentDevice_returns400Sdl() throws Exception {
            UserEntity user = signUp();

            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-A");
            ResultActions loginB = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-B");
            String atB = getAt(loginB);

            String currentDevicePublicId = getDeviceList(atB).stream()
                    .filter(d -> Boolean.TRUE.equals(d.get("currentDevice")))
                    .findFirst()
                    .map(d -> (String) d.get("publicId"))
                    .orElseThrow();

            mockMvc.perform(post("/api/v1/devices/{publicId}/logout", currentDevicePublicId)
                            .header("Authorization", "Bearer " + atB))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("SDL"));
        }

        @Test
        @DisplayName("존재하지 않는 publicId로 강제 로그아웃 시 404 NFD가 반환된다")
        void forceLogoutNonExistent_returns404Nfd() throws Exception {
            UserEntity user = signUp();
            ResultActions login = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);

            mockMvc.perform(post("/api/v1/devices/{publicId}/logout", NON_EXISTENT_PUBLIC_ID)
                            .header("Authorization", "Bearer " + getAt(login)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NFD"));
        }

        @Test
        @DisplayName("이미 로그아웃된 기기를 강제 로그아웃해도 200이 반환된다 — 멱등성")
        void forceLogoutAlreadyInactiveDevice_returns200() throws Exception {
            UserEntity user = signUp();

            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-A");
            ResultActions loginB = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-B");
            String atB = getAt(loginB);

            String publicIdA = getOtherDevicePublicId(atB);

            // 첫 번째 강제 로그아웃
            mockMvc.perform(post("/api/v1/devices/{publicId}/logout", publicIdA)
                            .header("Authorization", "Bearer " + atB))
                    .andExpect(status().isOk());

            // 이미 비활성화된 기기에 재요청 → 200 (멱등)
            mockMvc.perform(post("/api/v1/devices/{publicId}/logout", publicIdA)
                            .header("Authorization", "Bearer " + atB))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("디바이스 삭제")
    class DeviceDelete {

        @Test
        @DisplayName("비활성 디바이스를 삭제하면 DB에서 제거되고 목록에서 사라진다")
        void deleteInactiveDevice_removesFromDb() throws Exception {
            UserEntity user = signUp();

            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-A");
            ResultActions loginB = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-B");
            String atB = getAt(loginB);

            String publicIdA = getOtherDevicePublicId(atB);

            // 강제 로그아웃으로 비활성화
            mockMvc.perform(post("/api/v1/devices/{publicId}/logout", publicIdA)
                    .header("Authorization", "Bearer " + atB));

            mockMvc.perform(delete("/api/v1/devices/{publicId}", publicIdA)
                            .header("Authorization", "Bearer " + atB))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SU"));

            assertThat(deviceRepository.count()).isEqualTo(1L);

            List<Map<String, Object>> devices = getDeviceList(atB);
            boolean stillExists = devices.stream()
                    .anyMatch(d -> publicIdA.equals(d.get("publicId")));
            assertThat(stillExists).isFalse();
        }

        @Test
        @DisplayName("활성 세션이 있는 디바이스를 삭제 시도하면 409 ACD가 반환된다")
        void deleteActiveDevice_returns409Acd() throws Exception {
            UserEntity user = signUp();

            signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-A");
            ResultActions loginB = signInWithDevice(user.getLoginId(), UserFixture.RAW_PASSWORD, "fp-B");
            String atB = getAt(loginB);

            String publicIdA = getOtherDevicePublicId(atB);

            mockMvc.perform(delete("/api/v1/devices/{publicId}", publicIdA)
                            .header("Authorization", "Bearer " + atB))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ACD"));
        }

        @Test
        @DisplayName("존재하지 않는 publicId 삭제 시도 시 404 NFD가 반환된다")
        void deleteNonExistent_returns404Nfd() throws Exception {
            UserEntity user = signUp();
            ResultActions login = signIn(user.getLoginId(), UserFixture.RAW_PASSWORD);

            mockMvc.perform(delete("/api/v1/devices/{publicId}", NON_EXISTENT_PUBLIC_ID)
                            .header("Authorization", "Bearer " + getAt(login)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NFD"));
        }
    }

}