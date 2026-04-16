package com.han.back.global.security.util;

import com.han.back.domain.user.entity.Role;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.security.token.AuthConst;
import com.han.back.global.security.token.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil")
class JwtUtilTest {

    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2UtbWluaW11bS0yNTYtYml0cw==";

    private static final String TEST_ISSUER = "test-issuer";
    private static final Long USER_PK = 1L;
    private static final String SESSION_ID = "test-session-id";
    private static final long ACCESS_EXP_MS = 1_800_000L;   // 30분
    private static final long REFRESH_EXP_MS = 86_400_000L;  // 24시간

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "issuer", TEST_ISSUER);
    }

    private String createAccessToken() {
        return jwtUtil.createJwt(AuthConst.TOKEN_TYPE_ACCESS, USER_PK, Role.USER, SESSION_ID, ACCESS_EXP_MS);
    }

    private String createRefreshToken() {
        return jwtUtil.createJwt(AuthConst.TOKEN_TYPE_REFRESH, USER_PK, Role.USER, SESSION_ID, REFRESH_EXP_MS);
    }

    private String createExpiredToken(String category) {
        return jwtUtil.createJwt(category, USER_PK, Role.USER, SESSION_ID, -1L);
    }

    private JwtUtil createJwtUtilWithDifferentSecret() {
        String otherSecret = "b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdGluZy1taW5pbXVtLTI1Ni1iaXRzISE=";
        JwtUtil other = new JwtUtil(otherSecret);
        ReflectionTestUtils.setField(other, "issuer", TEST_ISSUER);
        return other;
    }

    private String buildUnsignedJwt(String headerJson, String payloadJson) {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(headerJson.getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes());
        return header + "." + payload + ".";
    }

    @Nested
    @DisplayName("createJwt()")
    class CreateJwt {

        @Test
        @DisplayName("AT 생성 시 category claim 이 'access' 이다")
        void accessToken_categoryClaimIsAccess() {
            Claims claims = jwtUtil.parseClaims(createAccessToken());

            assertThat(jwtUtil.getCategory(claims)).isEqualTo(AuthConst.TOKEN_TYPE_ACCESS);
        }

        @Test
        @DisplayName("RT 생성 시 category claim 이 'refresh' 이다")
        void refreshToken_categoryClaimIsRefresh() {
            Claims claims = jwtUtil.parseClaims(createRefreshToken());

            assertThat(jwtUtil.getCategory(claims)).isEqualTo(AuthConst.TOKEN_TYPE_REFRESH);
        }

        @Test
        @DisplayName("sid claim 이 전달한 sessionId 와 정확히 일치한다")
        void sessionId_isStoredInSidClaim() {
            Claims claims = jwtUtil.parseClaims(createAccessToken());

            assertThat(jwtUtil.getSessionId(claims)).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("role claim 이 ROLE_USER 형식으로 저장된다")
        void role_isStoredAsRoleAuthority() {
            Claims claims = jwtUtil.parseClaims(createAccessToken());

            assertThat(jwtUtil.getRole(claims)).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("id claim 이 전달한 userPk 와 일치한다")
        void userPk_isStoredInIdClaim() {
            Claims claims = jwtUtil.parseClaims(createAccessToken());

            assertThat(jwtUtil.getId(claims)).isEqualTo(USER_PK);
        }

        @Test
        @DisplayName("issuer claim 이 설정한 값과 일치한다")
        void issuer_isStoredCorrectly() {
            Claims claims = jwtUtil.parseClaims(createAccessToken());

            assertThat(claims.getIssuer()).isEqualTo(TEST_ISSUER);
        }
    }

    @Nested
    @DisplayName("parseClaims()")
    class ParseClaims {

        @Test
        @DisplayName("유효한 토큰은 claims 를 정상 반환한다")
        void validToken_returnsClaims() {
            Claims claims = jwtUtil.parseClaims(createAccessToken());

            assertThat(claims).isNotNull();
            assertThat(jwtUtil.getId(claims)).isEqualTo(USER_PK);
        }

        @Test
        @DisplayName("만료된 토큰은 EXPIRED_JWT_TOKEN 예외를 던진다")
        void expiredToken_throwsExpiredJwtToken() {
            assertThatThrownBy(() -> jwtUtil.parseClaims(createExpiredToken(AuthConst.TOKEN_TYPE_ACCESS)))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.EXPIRED_JWT_TOKEN);
        }

        @Test
        @DisplayName("다른 secret 으로 서명한 토큰은 INVALID_JWT_SIGNATURE 예외를 던진다")
        void tamperedToken_throwsInvalidSignature() {
            String foreignToken = createJwtUtilWithDifferentSecret().createJwt(
                    AuthConst.TOKEN_TYPE_ACCESS, USER_PK, Role.USER, SESSION_ID, ACCESS_EXP_MS
            );

            assertThatThrownBy(() -> jwtUtil.parseClaims(foreignToken))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.INVALID_JWT_SIGNATURE);
        }

        @Test
        @DisplayName("빈 문자열은 EMPTY_JWT_TOKEN 예외를 던진다")
        void emptyString_throwsEmptyJwtToken() {
            assertThatThrownBy(() -> jwtUtil.parseClaims(""))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.EMPTY_JWT_TOKEN);
        }

        @Test
        @DisplayName("형식이 손상된 토큰은 INVALID_JWT_SIGNATURE 예외를 던진다")
        void malformedToken_throwsInvalidSignature() {
            assertThatThrownBy(() -> jwtUtil.parseClaims("not.a.valid.jwt.token"))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.INVALID_JWT_SIGNATURE);
        }

        @Test
        @DisplayName("null 은 EMPTY_JWT_TOKEN 예외를 던진다")
        void nullToken_throwsEmptyJwtToken() {
            assertThatThrownBy(() -> jwtUtil.parseClaims(null))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.EMPTY_JWT_TOKEN);
        }

        @Test
        @DisplayName("서명되지 않은 토큰은 UNSUPPORTED_JWT_TOKEN 예외를 던진다")
        void unsignedToken_throwsUnsupportedJwtToken() {
            String unsignedToken = buildUnsignedJwt(
                    "{\"alg\":\"none\"}",
                    "{\"cat\":\"access\"}"
            );

            assertThatThrownBy(() -> jwtUtil.parseClaims(unsignedToken))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.UNSUPPORTED_JWT_TOKEN);
        }
    }

    @Nested
    @DisplayName("extractClaimsLeniently()")
    class ExtractClaimsLeniently {

        @Test
        @DisplayName("유효한 토큰에서 claims 를 정상 반환한다")
        void validToken_returnsClaims() {
            Optional<Claims> result = jwtUtil.extractClaimsLeniently(createRefreshToken());

            assertThat(result).isPresent();
            assertThat(jwtUtil.getId(result.get())).isEqualTo(USER_PK);
        }

        @Test
        @DisplayName("만료된 토큰에서도 claims 를 추출한다")
        void expiredToken_stillReturnsClaims() {
            Optional<Claims> result = jwtUtil.extractClaimsLeniently(createExpiredToken(AuthConst.TOKEN_TYPE_ACCESS));

            assertThat(result).isPresent();
            assertThat(jwtUtil.getId(result.get())).isEqualTo(USER_PK);
            assertThat(jwtUtil.getSessionId(result.get())).isEqualTo(SESSION_ID);
        }

        @Test
        @DisplayName("서명이 위조된 토큰은 Optional.empty() 를 반환한다")
        void tamperedToken_returnsEmpty() {
            Optional<Claims> result = jwtUtil.extractClaimsLeniently("forged.header.signature");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 문자열 및 null 은 Optional.empty() 를 반환한다")
        void blankString_returnsEmpty() {
            assertThat(jwtUtil.extractClaimsLeniently("")).isEmpty();
            assertThat(jwtUtil.extractClaimsLeniently("   ")).isEmpty();
            assertThat(jwtUtil.extractClaimsLeniently(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRemainingExpiration()")
    class GetRemainingExpiration {

        @Test
        @DisplayName("유효한 토큰의 잔여 만료시간은 0 보다 크다")
        void validToken_remainingTimeIsPositive() {
            Claims claims = jwtUtil.parseClaims(createAccessToken());

            long remaining = jwtUtil.getRemainingExpiration(claims);
            assertThat(remaining).isGreaterThan(ACCESS_EXP_MS - 5_000L);
        }

        @Test
        @DisplayName("이미 만료된 토큰의 잔여 만료시간은 0 이다")
        void expiredToken_remainingTimeIsZero() {
            Claims claims = jwtUtil.extractClaimsLeniently(createExpiredToken(AuthConst.TOKEN_TYPE_ACCESS)).orElseThrow();

            long remaining = jwtUtil.getRemainingExpiration(claims);

            assertThat(remaining).isZero();
        }
    }

}