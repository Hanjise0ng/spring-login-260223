package com.han.back.domain.auth.oauth2.adapter;

import com.han.back.domain.auth.oauth2.adapter.google.GoogleOAuth2UserAdapter;
import com.han.back.domain.auth.oauth2.adapter.kakao.KakaoOAuth2UserAdapter;
import com.han.back.domain.auth.oauth2.adapter.naver.NaverOAuth2UserAdapter;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.entity.AuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2UserInfo 어댑터 파싱")
class OAuth2UserAdapterTest {

    @Nested
    @DisplayName("KakaoOAuth2User")
    class Kakao {

        private OAuth2UserInfo create(Map<String, Object> attributes) {
            return new KakaoOAuth2UserAdapter().convert(attributes);
        }

        @Test
        @DisplayName("정상 응답: 모든 필드가 올바르게 파싱된다")
        void fullResponse_allFieldsParsed() {
            OAuth2UserInfo userInfo = create(Map.of(
                    "id", 1234567890L,
                    "kakao_account", Map.of(
                            "is_email_verified", true,
                            "email", "user@kakao.com",
                            "profile", Map.of("nickname", "카카오유저")
                    )
            ));

            assertThat(userInfo.getProvider()).isEqualTo(AuthProvider.KAKAO);
            assertThat(userInfo.getProviderId()).isEqualTo("1234567890");
            assertThat(userInfo.getEmail()).isEqualTo("user@kakao.com");
            assertThat(userInfo.getNickname()).isEqualTo("카카오유저");
        }

        @Test
        @DisplayName("이메일 미인증: getEmail()이 null을 반환한다")
        void emailNotVerified_returnsNull() {
            OAuth2UserInfo userInfo = create(Map.of(
                    "id", 1234567890L,
                    "kakao_account", Map.of(
                            "is_email_verified", false,
                            "email", "user@kakao.com",
                            "profile", Map.of("nickname", "카카오유저")
                    )
            ));

            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getNickname()).isEqualTo("카카오유저");
        }

        @Test
        @DisplayName("is_email_verified 키 없음: getEmail()이 null을 반환한다")
        void noVerifiedKey_returnsNull() {
            OAuth2UserInfo userInfo = create(Map.of(
                    "id", 1234567890L,
                    "kakao_account", Map.of(
                            "email", "user@kakao.com",
                            "profile", Map.of("nickname", "카카오유저")
                    )
            ));

            assertThat(userInfo.getEmail()).isNull();
        }

        @Test
        @DisplayName("kakao_account 없음: 이메일 null, 기본 닉네임")
        void noKakaoAccount_returnsDefaults() {
            OAuth2UserInfo userInfo = create(Map.of("id", 1234567890L));

            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getNickname()).isEqualTo(OAuth2Const.DEFAULT_NICKNAME);
            assertThat(userInfo.getProviderId()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("profile 없음: 이메일 정상, 기본 닉네임")
        void noProfile_returnsDefaultNickname() {
            OAuth2UserInfo userInfo = create(Map.of(
                    "id", 1234567890L,
                    "kakao_account", Map.of(
                            "is_email_verified", true,
                            "email", "user@kakao.com"
                    )
            ));

            assertThat(userInfo.getEmail()).isEqualTo("user@kakao.com");
            assertThat(userInfo.getNickname()).isEqualTo(OAuth2Const.DEFAULT_NICKNAME);
        }

        @Test
        @DisplayName("id가 Long: String으로 변환된다")
        void longId_convertedToString() {
            OAuth2UserInfo userInfo = create(Map.of("id", 9876543210L));

            assertThat(userInfo.getProviderId()).isEqualTo("9876543210");
        }
    }

    @Nested
    @DisplayName("NaverOAuth2User")
    class Naver {

        private OAuth2UserInfo create(Map<String, Object> attributes) {
            return new NaverOAuth2UserAdapter().convert(attributes);
        }

        @Test
        @DisplayName("정상 응답: response 래퍼에서 필드를 추출한다")
        void fullResponse_allFieldsParsed() {
            OAuth2UserInfo userInfo = create(Map.of(
                    "resultcode", "00",
                    "response", Map.of(
                            "id", "32742776",
                            "name", "네이버유저",
                            "email", "user@naver.com"
                    )
            ));

            assertThat(userInfo.getProvider()).isEqualTo(AuthProvider.NAVER);
            assertThat(userInfo.getProviderId()).isEqualTo("32742776");
            assertThat(userInfo.getEmail()).isEqualTo("user@naver.com");
            assertThat(userInfo.getNickname()).isEqualTo("네이버유저");
        }

        @Test
        @DisplayName("response 키 없음: 모든 필드 null 또는 기본값")
        void noResponse_returnsDefaults() {
            OAuth2UserInfo userInfo = create(Map.of("resultcode", "00"));

            assertThat(userInfo.getProviderId()).isNull();
            assertThat(userInfo.getEmail()).isNull();
            assertThat(userInfo.getNickname()).isEqualTo(OAuth2Const.DEFAULT_NICKNAME);
        }

        @Test
        @DisplayName("name 없음: 기본 닉네임, 다른 필드 정상")
        void noName_returnsDefaultNickname() {
            OAuth2UserInfo userInfo = create(Map.of(
                    "response", Map.of("id", "12345", "email", "user@naver.com")
            ));

            assertThat(userInfo.getNickname()).isEqualTo(OAuth2Const.DEFAULT_NICKNAME);
            assertThat(userInfo.getEmail()).isEqualTo("user@naver.com");
        }
    }

    @Nested
    @DisplayName("GoogleOAuth2User")
    class Google {

        private OAuth2UserInfo create(Map<String, Object> attributes) {
            return new GoogleOAuth2UserAdapter().convert(attributes);
        }

        @Test
        @DisplayName("정상 응답: flat 구조에서 필드를 추출한다")
        void fullResponse_allFieldsParsed() {
            OAuth2UserInfo userInfo = create(Map.of(
                    "sub", "110248495921238986420",
                    "name", "구글유저",
                    "email", "user@gmail.com"
            ));

            assertThat(userInfo.getProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(userInfo.getProviderId()).isEqualTo("110248495921238986420");
            assertThat(userInfo.getEmail()).isEqualTo("user@gmail.com");
            assertThat(userInfo.getNickname()).isEqualTo("구글유저");
        }

        @Test
        @DisplayName("name 없음: 기본 닉네임")
        void noName_returnsDefaultNickname() {
            OAuth2UserInfo userInfo = create(Map.of("sub", "12345", "email", "user@gmail.com"));

            assertThat(userInfo.getNickname()).isEqualTo(OAuth2Const.DEFAULT_NICKNAME);
            assertThat(userInfo.getEmail()).isEqualTo("user@gmail.com");
        }
    }

}