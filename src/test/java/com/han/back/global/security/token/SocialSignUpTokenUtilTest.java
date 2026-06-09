package com.han.back.global.security.token;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.token.util.JwtUtil;
import com.han.back.global.security.token.util.SocialSignUpTokenUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SocialSignUpTokenUtil")
class SocialSignUpTokenUtilTest {

    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2UtbWluaW11bS0yNTYtYml0cw==";
    private static final String TEST_ISSUER = "test-issuer";

    private JwtUtil jwtUtil;
    private SocialSignUpTokenUtil SocialSignUpTokenUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "issuer", TEST_ISSUER);
        SocialSignUpTokenUtil = new SocialSignUpTokenUtil(jwtUtil);
    }

    @Nested
    @DisplayName("issue() + validate()")
    class IssueAndValidate {

        @Test
        @DisplayName("issue → validate 라운드트립: claims 값이 일치한다")
        void roundTrip_claimsMatch() {
            String token = SocialSignUpTokenUtil.issue("KAKAO", "123456", "카카오유저");
            SocialSignUpClaims claims = SocialSignUpTokenUtil.validate(token);

            assertThat(claims.getProvider()).isEqualTo("KAKAO");
            assertThat(claims.getProviderId()).isEqualTo("123456");
            assertThat(claims.getNickname()).isEqualTo("카카오유저");
        }

        @Test
        @DisplayName("닉네임에 특수문자(콜론, 해시) 포함 시 정상 파싱")
        void specialCharsInNickname_parsedCorrectly() {
            String token = SocialSignUpTokenUtil.issue("KAKAO", "123", "유저:닉네임#특수");
            SocialSignUpClaims claims = SocialSignUpTokenUtil.validate(token);

            assertThat(claims.getNickname()).isEqualTo("유저:닉네임#특수");
        }

        @Test
        @DisplayName("서로 다른 Provider로 발급한 토큰이 각각 정확한 claims를 반환한다")
        void differentProviders_eachReturnCorrectClaims() {
            String kakaoToken = SocialSignUpTokenUtil.issue("KAKAO", "111", "카카오");
            String googleToken = SocialSignUpTokenUtil.issue("GOOGLE", "222", "구글");

            SocialSignUpClaims kakaoClaims = SocialSignUpTokenUtil.validate(kakaoToken);
            SocialSignUpClaims googleClaims = SocialSignUpTokenUtil.validate(googleToken);

            assertThat(kakaoClaims.getProvider()).isEqualTo("KAKAO");
            assertThat(googleClaims.getProvider()).isEqualTo("GOOGLE");
        }
    }

    @Nested
    @DisplayName("validate() 실패 케이스")
    class ValidateFailure {

        @Test
        @DisplayName("다른 category 토큰 → STI 예외")
        void wrongCategory_throwsSTI() {
            String wrongToken = jwtUtil.createTempToken(
                    "wrong_category", 60_000L, Map.of());

            assertThatThrownBy(() -> SocialSignUpTokenUtil.validate(wrongToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_SIGNUP_TOKEN_INVALID);
        }

        @Test
        @DisplayName("만료된 토큰 → STI 예외 (parseClaims가 EXPIRED를 던지고 catch에서 STI로 변환)")
        void expiredToken_throwsSTI() {
            String expiredToken = jwtUtil.createTempToken(
                    OAuth2Const.TOKEN_CATEGORY_SOCIAL_SIGN_UP,
                    -1L,
                    Map.of(
                            AuthConst.TEMP_PROVIDER, "KAKAO",
                            AuthConst.TEMP_PROVIDER_ID, "123",
                            "nickname", "유저"
                    ));

            assertThatThrownBy(() -> SocialSignUpTokenUtil.validate(expiredToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_SIGNUP_TOKEN_INVALID);
        }

        @Test
        @DisplayName("다른 secret으로 서명된 토큰 → STI 예외")
        void tamperedToken_throwsSTI() {
            JwtUtil otherJwtUtil = new JwtUtil(
                    "b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdGluZy1taW5pbXVtLTI1Ni1iaXRzISE=");
            ReflectionTestUtils.setField(otherJwtUtil, "issuer", TEST_ISSUER);

            String tamperedToken = otherJwtUtil.createTempToken(
                    OAuth2Const.TOKEN_CATEGORY_SOCIAL_SIGN_UP,
                    60_000L,
                    Map.of(
                            AuthConst.TEMP_PROVIDER, "KAKAO",
                            AuthConst.TEMP_PROVIDER_ID, "123",
                            "nickname", "유저"
                    ));

            assertThatThrownBy(() -> SocialSignUpTokenUtil.validate(tamperedToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_SIGNUP_TOKEN_INVALID);
        }

        @Test
        @DisplayName("완전히 잘못된 문자열 → STI 예외")
        void malformedString_throwsSTI() {
            assertThatThrownBy(() -> SocialSignUpTokenUtil.validate("not.a.valid.jwt"))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_SIGNUP_TOKEN_INVALID);
        }

        @Test
        @DisplayName("null 토큰 → STI 예외")
        void nullToken_throwsSTI() {
            assertThatThrownBy(() -> SocialSignUpTokenUtil.validate(null))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_SIGNUP_TOKEN_INVALID);
        }
    }

    @Nested
    @DisplayName("createTempToken() 통합 검증")
    class CreateTempToken {

        @Test
        @DisplayName("생성된 토큰의 category claim이 지정한 값과 일치한다")
        void category_matchesSpecifiedValue() {
            String token = jwtUtil.createTempToken("test_category", 60_000L, Map.of());
            Claims claims = jwtUtil.parseClaims(token);

            assertThat(jwtUtil.getCategory(claims)).isEqualTo("test_category");
        }

        @Test
        @DisplayName("extraClaims의 모든 키-값이 토큰에 포함된다")
        void extraClaims_allIncludedInToken() {
            Map<String, Object> extra = Map.of("key1", "value1", "key2", "value2");
            String token = jwtUtil.createTempToken("test", 60_000L, extra);
            Claims claims = jwtUtil.parseClaims(token);

            assertThat(claims.get("key1", String.class)).isEqualTo("value1");
            assertThat(claims.get("key2", String.class)).isEqualTo("value2");
        }

        @Test
        @DisplayName("issuer가 설정된 값과 일치한다")
        void issuer_matchesConfigured() {
            String token = jwtUtil.createTempToken("test", 60_000L, Map.of());
            Claims claims = jwtUtil.parseClaims(token);

            assertThat(claims.getIssuer()).isEqualTo(TEST_ISSUER);
        }
    }

}