package com.han.back.global.security.service.implement;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.JwtUtil;
import com.han.back.global.infra.redis.util.RedisUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenServiceImpl")
class TokenServiceImplTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private RedisUtil redisUtil;

    @InjectMocks private TokenServiceImpl tokenService;

    private static final Long USER_PK = 1L;
    private static final Role ROLE = Role.USER;
    private static final String SESSION_ID = "session-abc-123";
    private static final String FAKE_AT = "fake.access.token";
    private static final String FAKE_RT = "fake.refresh.token";

    private static final String RT_KEY = "refresh:1:session-abc-123";
    private static final String BLACKLIST_KEY = "blacklist:session:session-abc-123";

    @Captor private ArgumentCaptor<String> keyCaptor;
    @Captor private ArgumentCaptor<String> valueCaptor;
    @Captor private ArgumentCaptor<Long> ttlCaptor;

    // createJwt AT/RT 양방향 Stub — SESSION_ID 기준
    private void stubJwtCreate() {
        given(jwtUtil.createJwt(
                eq(AuthConst.TOKEN_TYPE_ACCESS), eq(USER_PK), eq(ROLE), eq(SESSION_ID), anyLong()))
                .willReturn(FAKE_AT);
        given(jwtUtil.createJwt(
                eq(AuthConst.TOKEN_TYPE_REFRESH), eq(USER_PK), eq(ROLE), eq(SESSION_ID), anyLong()))
                .willReturn(FAKE_RT);
    }

    // category claim만 stub — 예외 흐름 테스트 전용
    private Claims claimsMock(String category) {
        Claims claims = mock(Claims.class);
        given(jwtUtil.getCategory(claims)).willReturn(category);
        return claims;
    }

    // 모든 필드 stub — 정상 흐름 테스트 전용
    private Claims fullClaimsMock(String category) {
        Claims claims = claimsMock(category);
        given(jwtUtil.getId(claims)).willReturn(USER_PK);
        given(jwtUtil.getRole(claims)).willReturn(ROLE);
        given(jwtUtil.getSessionId(claims)).willReturn(SESSION_ID);
        return claims;
    }

    @Nested
    @DisplayName("issueTokens()")
    class IssueTokens {

        @Test
        @DisplayName("AT와 RT를 포함한 AuthTokenDto를 반환한다")
        void returnsAuthTokenDto_withBothTokens() {
            stubJwtCreate();

            AuthToken result = tokenService.issueTokens(USER_PK, ROLE, SESSION_ID);

            assertThat(result.getAccessToken()).isEqualTo(FAKE_AT);
            assertThat(result.getRefreshToken()).isEqualTo(FAKE_RT);
        }

        @Test
        @DisplayName("RT가 Redis에 'refresh:{pk}:{sessionId}'키로 저장된다")
        void storesRt_withCorrectKeyFormat() {
            stubJwtCreate();

            tokenService.issueTokens(USER_PK, ROLE, SESSION_ID);

            then(redisUtil).should(times(1))
                    .setDataExpire(keyCaptor.capture(), valueCaptor.capture(), ttlCaptor.capture());

            assertThat(keyCaptor.getValue()).isEqualTo(RT_KEY);
            assertThat(valueCaptor.getValue()).isEqualTo(FAKE_RT);
        }

        @Test
        @DisplayName("RT TTL은 AuthConst.REFRESH_EXPIRATION이다")
        void storesRt_withRefreshExpirationTtl() {
            stubJwtCreate();

            tokenService.issueTokens(USER_PK, ROLE, SESSION_ID);

            then(redisUtil).should().setDataExpire(any(), any(), ttlCaptor.capture());
            assertThat(ttlCaptor.getValue()).isEqualTo(AuthConst.REFRESH_EXPIRATION);
        }

        @Test
        @DisplayName("AT는 Redis에 저장하지 않는다 — setDataExpire는 정확히 1번만 호출된다")
        void doesNotStore_accessTokenInRedis() {
            stubJwtCreate();

            tokenService.issueTokens(USER_PK, ROLE, SESSION_ID);

            then(redisUtil).should(times(1))
                    .setDataExpire(eq(RT_KEY), eq(FAKE_RT), eq(AuthConst.REFRESH_EXPIRATION));
        }
    }

    @Nested
    @DisplayName("invalidateSession()")
    class InvalidateSession {

        @Test
        @DisplayName("블랙리스트 키가 'blacklist:session:{sessionId}' 형식이다")
        void blacklistKey_hasCorrectFormat() {
            tokenService.invalidateSession(USER_PK, SESSION_ID);

            then(redisUtil).should()
                    .setDataExpire(keyCaptor.capture(), eq("revoked"), anyLong());
            assertThat(keyCaptor.getValue()).isEqualTo(BLACKLIST_KEY);
        }

        @Test
        @DisplayName("블랙리스트 TTL은 AuthConst.ACCESS_EXPIRATION이다")
        void blacklistTtl_equalsAccessExpiration() {
            tokenService.invalidateSession(USER_PK, SESSION_ID);

            then(redisUtil).should()
                    .setDataExpire(startsWith("blacklist:"), any(), ttlCaptor.capture());
            assertThat(ttlCaptor.getValue()).isEqualTo(AuthConst.ACCESS_EXPIRATION);
        }

        @Test
        @DisplayName("RT가 Redis에서 삭제된다")
        void deletesRt_fromRedis() {
            tokenService.invalidateSession(USER_PK, SESSION_ID);

            then(redisUtil).should(times(1)).deleteData(RT_KEY);
        }
    }

    @Nested
    @DisplayName("rotateTokens()")
    class RotateTokens {

        private static final String OLD_SESSION = "old-session-id";
        private static final String NEW_SESSION = "new-session-id";

        private static final String NEW_AT = "new.fake.access.token";
        private static final String NEW_RT = "new.fake.refresh.token";

        @BeforeEach
        void stubNewSessionTokens() {
            given(jwtUtil.createJwt(
                    eq(AuthConst.TOKEN_TYPE_ACCESS), eq(USER_PK), eq(ROLE), eq(NEW_SESSION), anyLong()))
                    .willReturn(NEW_AT);
            given(jwtUtil.createJwt(
                    eq(AuthConst.TOKEN_TYPE_REFRESH), eq(USER_PK), eq(ROLE), eq(NEW_SESSION), anyLong()))
                    .willReturn(NEW_RT);
        }

        @Test
        @DisplayName("이전 세션을 블랙리스트에 등록하고 이전 RT를 삭제한다")
        void invalidatesOldSession() {
            tokenService.rotateTokens(USER_PK, ROLE, OLD_SESSION, NEW_SESSION);

            then(redisUtil).should().setDataExpire(
                    eq("blacklist:session:" + OLD_SESSION), eq("revoked"), anyLong());
            then(redisUtil).should().deleteData("refresh:" + USER_PK + ":" + OLD_SESSION);
        }

        @Test
        @DisplayName("새 sessionId로 RT가 Redis에 저장된다")
        void storesNewRt_withNewSessionId() {
            tokenService.rotateTokens(USER_PK, ROLE, OLD_SESSION, NEW_SESSION);

            then(redisUtil).should().setDataExpire(
                    eq("refresh:" + USER_PK + ":" + NEW_SESSION), eq(NEW_RT), anyLong());
        }

        @Test
        @DisplayName("새 sessionId로 발급된 AT/RT를 반환한다")
        void returnsNewTokens_withNewSessionId() {
            AuthToken result = tokenService.rotateTokens(USER_PK, ROLE, OLD_SESSION, NEW_SESSION);

            assertThat(result.getAccessToken()).isEqualTo(NEW_AT);
            assertThat(result.getRefreshToken()).isEqualTo(NEW_RT);
        }
    }

    @Nested
    @DisplayName("authenticateAccessToken()")
    class AuthenticateAccessToken {

        @Test
        @DisplayName("유효한 AT로 CustomUserDetails를 반환한다")
        void validToken_returnsCustomUserDetails() {
            // 정상 흐름: category → sessionId → blacklist 확인 → id → role 순서로 모두 호출
            Claims claims = fullClaimsMock(AuthConst.TOKEN_TYPE_ACCESS);
            given(jwtUtil.parseClaims(FAKE_AT)).willReturn(claims);
            given(redisUtil.hasKey(BLACKLIST_KEY)).willReturn(false);

            CustomUserDetails result = tokenService.authenticateAccessToken(FAKE_AT);

            assertThat(result.getId()).isEqualTo(USER_PK);
            assertThat(result.getRole()).isEqualTo(ROLE);
            assertThat(result.getSessionId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("블랙리스트에 등록된 세션은 AUTHENTICATION_FAIL 예외를 던진다")
        void blacklistedSession_throwsAuthenticationFail() {
            // 실행 경로: category → sessionId → blacklist(true) → throw
            Claims claims = claimsMock(AuthConst.TOKEN_TYPE_ACCESS);
            given(jwtUtil.getSessionId(claims)).willReturn(SESSION_ID);
            given(jwtUtil.parseClaims(FAKE_AT)).willReturn(claims);
            given(redisUtil.hasKey(BLACKLIST_KEY)).willReturn(true);

            assertThatThrownBy(() -> tokenService.authenticateAccessToken(FAKE_AT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        @Test
        @DisplayName("category가 'access'가 아닌 토큰은 UNSUPPORTED_JWT_TOKEN 예외를 던진다")
        void refreshTokenUsedAsAt_throwsUnsupportedJwtToken() {
            // 실행 경로: category 확인 → 즉시 throw
            Claims claims = claimsMock(AuthConst.TOKEN_TYPE_REFRESH);
            given(jwtUtil.parseClaims(FAKE_AT)).willReturn(claims);

            assertThatThrownBy(() -> tokenService.authenticateAccessToken(FAKE_AT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }

        @Test
        @DisplayName("Redis 장애 시 보안 우선(fail-closed) — AUTHENTICATION_FAIL 예외를 던진다")
        void redisFailure_failClosed_throwsAuthenticationFail() {
            // 실행 경로: category → sessionId → Redis 장애 → throw
            Claims claims = claimsMock(AuthConst.TOKEN_TYPE_ACCESS);
            given(jwtUtil.getSessionId(claims)).willReturn(SESSION_ID);
            given(jwtUtil.parseClaims(FAKE_AT)).willReturn(claims);
            given(redisUtil.hasKey(BLACKLIST_KEY))
                    .willThrow(new CustomException(BaseResponseStatus.REDIS_ERROR));

            assertThatThrownBy(() -> tokenService.authenticateAccessToken(FAKE_AT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
    }

    @Nested
    @DisplayName("authenticateRefreshToken()")
    class AuthenticateRefreshToken {

        @Test
        @DisplayName("유효한 RT로 CustomUserDetails 를 반환한다")
        void validRt_returnsCustomUserDetails() {
            // 정상 흐름: category → id → role → sessionId 모두 호출
            Claims claims = fullClaimsMock(AuthConst.TOKEN_TYPE_REFRESH);
            given(jwtUtil.parseClaims(FAKE_RT)).willReturn(claims);

            CustomUserDetails result = tokenService.authenticateRefreshToken(FAKE_RT);

            assertThat(result.getId()).isEqualTo(USER_PK);
            assertThat(result.getSessionId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("category가 'refresh'가 아닌 토큰은 UNSUPPORTED_JWT_TOKEN 예외를 던진다")
        void accessTokenUsedAsRt_throwsUnsupportedJwtToken() {
            // 실행 경로: category 확인 → 즉시 throw
            Claims claims = claimsMock(AuthConst.TOKEN_TYPE_ACCESS);
            given(jwtUtil.parseClaims(FAKE_RT)).willReturn(claims);

            assertThatThrownBy(() -> tokenService.authenticateRefreshToken(FAKE_RT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }
    }

    @Nested
    @DisplayName("validateRefreshToken()")
    class ValidateRefreshToken {

        @Test
        @DisplayName("Redis 저장값과 일치하는 RT는 예외 없이 통과한다")
        void matchingRt_doesNotThrow() {
            given(redisUtil.getData(RT_KEY)).willReturn(Optional.of(FAKE_RT));

            assertThatCode(() -> tokenService.validateRefreshToken(USER_PK, SESSION_ID, FAKE_RT))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Redis 저장값과 불일치하는 RT는 AUTHENTICATION_FAIL 예외를 던진다")
        void mismatchedRt_throwsAuthenticationFail() {
            given(redisUtil.getData(RT_KEY)).willReturn(Optional.of("completely.different.rt"));

            assertThatThrownBy(() ->
                    tokenService.validateRefreshToken(USER_PK, SESSION_ID, FAKE_RT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);
        }

        @Test
        @DisplayName("Redis에 RT가 없으면 AUTHENTICATION_FAIL 예외를 던진다 — 세션 탈취 의심")
        void noRtInRedis_throwsAuthenticationFail() {
            given(redisUtil.getData(RT_KEY)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    tokenService.validateRefreshToken(USER_PK, SESSION_ID, FAKE_RT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);
        }
    }

    @Nested
    @DisplayName("extractUserFromTokens()")
    class ExtractUserFromTokens {

        @Test
        @DisplayName("AT 만료 + 유효한 RT → RT에서 사용자 정보를 추출한다")
        void expiredAt_validRt_extractsFromRt() {
            // AT 파싱 실패 → orElseGet 분기 → getSessionId(rtClaims) 호출됨
            Claims rtClaims = fullClaimsMock(AuthConst.TOKEN_TYPE_REFRESH);
            given(jwtUtil.extractClaimsLeniently(FAKE_AT)).willReturn(Optional.empty());
            given(jwtUtil.extractClaimsLeniently(FAKE_RT)).willReturn(Optional.of(rtClaims));

            Optional<CustomUserDetails> result =
                    tokenService.extractUserFromTokens(FAKE_AT, FAKE_RT);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(USER_PK);
            assertThat(result.get().getSessionId()).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("AT와 RT 모두 파싱 불가 → Optional.empty() 반환")
        void bothTokensInvalid_returnsEmpty() {
            given(jwtUtil.extractClaimsLeniently(any())).willReturn(Optional.empty());

            Optional<CustomUserDetails> result =
                    tokenService.extractUserFromTokens(FAKE_AT, FAKE_RT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("RT category 가 'refresh'가 아니면 Optional.empty() 반환")
        void rtWithWrongCategory_returnsEmpty() {
            // 실행 경로: extractLeniently(RT) 성공 → getCategory → "access" → 즉시 return empty
            Claims fakeClaims = claimsMock(AuthConst.TOKEN_TYPE_ACCESS);
            given(jwtUtil.extractClaimsLeniently(FAKE_RT)).willReturn(Optional.of(fakeClaims));

            Optional<CustomUserDetails> result =
                    tokenService.extractUserFromTokens(FAKE_AT, FAKE_RT);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("AT도 유효하면 AT의 sessionId를 우선 사용한다")
        void bothValid_usesAtSessionId() {
            String atSessionId = "at-session-id";

            // AT: sessionId 제공
            Claims atClaims = mock(Claims.class);
            given(jwtUtil.getSessionId(atClaims)).willReturn(atSessionId);

            // RT: id/role 제공
            Claims rtClaims = mock(Claims.class);
            given(jwtUtil.getCategory(rtClaims)).willReturn(AuthConst.TOKEN_TYPE_REFRESH);
            given(jwtUtil.getId(rtClaims)).willReturn(USER_PK);
            given(jwtUtil.getRole(rtClaims)).willReturn(ROLE);

            given(jwtUtil.extractClaimsLeniently(FAKE_AT)).willReturn(Optional.of(atClaims));
            given(jwtUtil.extractClaimsLeniently(FAKE_RT)).willReturn(Optional.of(rtClaims));

            Optional<CustomUserDetails> result =
                    tokenService.extractUserFromTokens(FAKE_AT, FAKE_RT);

            assertThat(result).isPresent();
            assertThat(result.get().getSessionId()).isEqualTo(atSessionId);
        }
    }

}