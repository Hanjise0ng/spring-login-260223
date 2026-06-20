package com.han.back.global.security.token.util;

import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SocialLinkTokenUtil")
class SocialLinkTokenUtilTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2UtbWluaW11bS0yNTYtYml0cw==";
    private static final String TEST_ISSUER = "test_issuer";

    private JwtUtil jwtUtil;
    private SocialLinkTokenUtil socialLinkTokenUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "issuer", TEST_ISSUER);
        socialLinkTokenUtil = new SocialLinkTokenUtil(jwtUtil);
    }

    @Nested
    @DisplayName("issue() + validate()")
    class IssueAndValidate {

        @Test
        @DisplayName("발급한 토큰을 검증하면 userId를 복원한다")
        void roundTrip() {
            String token = socialLinkTokenUtil.issue(42L);
            Long userId = socialLinkTokenUtil.validate(token);

            assertThat(userId).isEqualTo(42L);
        }

        @Test
        @DisplayName("서로 다른 userId로 발급한 토큰이 각각 정확한 값을 반환한다")
        void distinctUserIds() {
            String tokenA = socialLinkTokenUtil.issue(1L);
            String tokenB = socialLinkTokenUtil.issue(999L);

            assertThat(socialLinkTokenUtil.validate(tokenA)).isEqualTo(1L);
            assertThat(socialLinkTokenUtil.validate(tokenB)).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("validate() 실패")
    class ValidateFailure {

        @Test
        @DisplayName("잘못된 문자열이면 SOCIAL_LINK_TOKEN_INVALID")
        void malformed_throws() {
            assertThatThrownBy(() -> socialLinkTokenUtil.validate("not-a-real-token"))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID);
        }

        @Test
        @DisplayName("null이면 SOCIAL_LINK_TOKEN_INVALID")
        void nullToken_throws() {
            assertThatThrownBy(() -> socialLinkTokenUtil.validate(null))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID);
        }

        @Test
        @DisplayName("다른 secret으로 서명된 토큰이면 SOCIAL_LINK_TOKEN_INVALID")
        void differentSecret_throws() {
            JwtUtil otherJwtUtil = new JwtUtil("b3RoZXItc2VjcmV0LWtleS1mb3ItdGVzdGluZy1wdXJwb3NlLW1pbmltdW0tMjU2LWJpdHM=");
            ReflectionTestUtils.setField(otherJwtUtil, "issuer", TEST_ISSUER);
            SocialLinkTokenUtil otherUtil = new SocialLinkTokenUtil(otherJwtUtil);
            String foreignToken = otherUtil.issue(42L);

            assertThatThrownBy(() -> socialLinkTokenUtil.validate(foreignToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID);
        }

        @Test
        @DisplayName("다른 category 토큰(가입 토큰)은 연동 토큰으로 검증되지 않는다")
        void wrongCategory_throws() {
            SocialSignUpTokenUtil signUpUtil = new SocialSignUpTokenUtil(jwtUtil);
            String signUpToken = signUpUtil.issue("KAKAO", "kakao-1", "닉네임");

            assertThatThrownBy(() -> socialLinkTokenUtil.validate(signUpToken))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID);
        }
    }

}