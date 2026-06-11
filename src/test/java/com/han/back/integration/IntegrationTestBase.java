package com.han.back.integration;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.fixture.UserFixture;
import com.han.back.global.security.token.AuthConst;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected CredentialRepository credentialRepository;
    @Autowired protected DeviceRepository deviceRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @MockitoBean protected JavaMailSender javaMailSender;

    @Autowired
    @Qualifier("customStringRedisTemplate")
    protected RedisTemplate<String, String> redisTemplate;

    // 테스트가 생성하는 Redis 키 접두사 목록
    private static final String[] TEST_KEY_PREFIXES = {
            AuthConst.TOKEN_REFRESH_REDIS_PREFIX,
            AuthConst.TOKEN_SESSION_BLACKLIST_PREFIX,
            "verification:",
    };

    @BeforeEach
    void setUpMailSender() {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(javaMailSender).send(any(MimeMessage.class));
    }

    @AfterEach
    void cleanUp() {
        deviceRepository.deleteAll();
        credentialRepository.deleteAll();  // 자식(FK: user_id) 먼저 삭제
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
     flushAll() 대신 테스트가 생성한 키만 선택적으로 삭제.
     테스트 병렬 실행 시 다른 테스트의 Redis 데이터를 보호한다.
     */
    private void cleanUpRedisTestKeys() {
        for (String prefix : TEST_KEY_PREFIXES) {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        }
    }

    /**
     기본 LOCAL 사용자 가입 — user 신원 + LOCAL credential을 함께 생성한다.
     loginId는 UserFixture.DEFAULT_LOGIN_ID 상수를 사용하므로,
     이 헬퍼로 가입한 사용자는 해당 상수로 로그인한다.
     */
    protected UserEntity signUp() {
        UserEntity user = userRepository.save(UserFixture.localUser());
        credentialRepository.save(
                UserFixture.localCredential(user.getId(), UserFixture.DEFAULT_LOGIN_ID, passwordEncoder));
        return user;
    }

    protected UserEntity signUpAs(String loginId, String email) {
        UserEntity user = userRepository.save(UserEntity.builder()
                .nickname(loginId)
                .tag(deriveTag(loginId))
                .email(email)
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build());

        credentialRepository.save(CredentialEntity.builder()
                .userId(user.getId())
                .provider(AuthProvider.LOCAL)
                .identifier(loginId)
                .password(passwordEncoder.encode(UserFixture.RAW_PASSWORD))
                .build());

        return user;
    }

    private static String deriveTag(String loginId) {
        int h = Math.abs(loginId.hashCode() % 10000);
        return String.format("%04d", h);
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