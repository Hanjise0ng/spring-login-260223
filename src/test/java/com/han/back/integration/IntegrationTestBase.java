package com.han.back.integration;

import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.fixture.UserFixture;
import com.han.back.global.security.token.AuthConst;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected DeviceRepository deviceRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @Autowired
    @Qualifier("customStringRedisTemplate")
    protected RedisTemplate<String, String> redisTemplate;

    // 테스트가 생성하는 Redis 키 접두사 목록
    private static final String[] TEST_KEY_PREFIXES = {
            AuthConst.TOKEN_REFRESH_REDIS_PREFIX,
            AuthConst.TOKEN_SESSION_BLACKLIST_PREFIX,
            "verification:",
    };

    @AfterEach
    void cleanUp() {
        deviceRepository.deleteAll();
        userRepository.deleteAll();
        cleanUpRedisTestKeys();
    }

    protected void flushRedis() {
        redisTemplate.getConnectionFactory()
                .getConnection()
                .serverCommands()
                .flushAll();
    }

    /**
     * flushAll() 대신 테스트가 생성한 키만 선택적으로 삭제.
     * 테스트 병렬 실행 시 다른 테스트의 Redis 데이터를 보호한다.
     */
    private void cleanUpRedisTestKeys() {
        for (String prefix : TEST_KEY_PREFIXES) {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    protected UserEntity signUp() {
        return userRepository.save(UserFixture.localUser(passwordEncoder));
    }

    protected UserEntity signUpAs(String loginId, String email) {
        return userRepository.save(UserEntity.builder()
                .loginId(loginId)
                .password(passwordEncoder.encode(UserFixture.RAW_PASSWORD))
                .email(email)
                .nickname(loginId)
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build());
    }

    /** 기본 로그인 요청 */
    protected ResultActions signIn(String loginId, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        Map.of("loginId", loginId, "password", password))));
    }

    /** device_id 쿠키를 포함한 로그인 (같은 기기 재로그인 시뮬레이션) */
    protected ResultActions signInWithDevice(
            String loginId, String password, String deviceId) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .cookie(new Cookie(AuthConst.COOKIE_DEVICE_ID_NAME, deviceId))
                .content(objectMapper.writeValueAsString(
                        Map.of("loginId", loginId, "password", password))));
    }

    /** AT + RT 포함 로그인 (이전 세션 무효화 진행됨) */
    protected ResultActions reSignIn(
            String loginId, String password, String accessToken, String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, refreshToken))
                .content(objectMapper.writeValueAsString(
                        Map.of("loginId", loginId, "password", password))));
    }

    /** AT + RT + device_id 포함 로그인 — 이전 세션 무효화 + 같은 기기 시뮬레이션 */
    protected ResultActions reSignInWithDevice(
            String loginId, String password, String accessToken, String refreshToken, String deviceId) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .cookie(new Cookie(AuthConst.COOKIE_REFRESH_TOKEN_NAME, refreshToken))
                .cookie(new Cookie(AuthConst.COOKIE_DEVICE_ID_NAME, deviceId))
                .content(objectMapper.writeValueAsString(
                        Map.of("loginId", loginId, "password", password))));
    }

    protected String getAt(ResultActions result) throws Exception {
        String header = result.andReturn().getResponse().getHeader("Authorization");
        return header != null ? header.replace("Bearer ", "") : null;
    }

    protected String getCookieValue(ResultActions result, String cookieName) throws Exception {
        Cookie cookie = result.andReturn().getResponse().getCookie(cookieName);
        return cookie != null ? cookie.getValue() : null;
    }

    protected Set<String> getRedisKeys(String pattern) {
        return redisTemplate.keys(pattern);
    }

    protected String getRedisValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    protected Set<String> getRtKeys(Long userId) {
        return getRedisKeys(AuthConst.TOKEN_REFRESH_REDIS_PREFIX + userId + ":*");
    }

    protected boolean isBlacklisted(String sessionId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(AuthConst.TOKEN_SESSION_BLACKLIST_PREFIX + sessionId));
    }

}