package com.han.back.domain.auth.oauth2.adapter;

import com.han.back.domain.auth.oauth2.adapter.google.GoogleOAuth2User;
import com.han.back.domain.auth.oauth2.adapter.google.GoogleOAuth2UserAdapter;
import com.han.back.domain.auth.oauth2.adapter.kakao.KakaoOAuth2User;
import com.han.back.domain.auth.oauth2.adapter.kakao.KakaoOAuth2UserAdapter;
import com.han.back.domain.auth.oauth2.adapter.naver.NaverOAuth2User;
import com.han.back.domain.auth.oauth2.adapter.naver.NaverOAuth2UserAdapter;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OAuth2UserResolver")
class OAuth2UserResolverTest {

    private OAuth2UserResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new OAuth2UserResolver(List.of(
                new KakaoOAuth2UserAdapter(),
                new NaverOAuth2UserAdapter(),
                new GoogleOAuth2UserAdapter()
        ));
    }

    @Test
    @DisplayName("kakao → KakaoOAuth2User를 반환한다")
    void kakao_returnsKakaoUser() {
        OAuth2UserInfo result = resolver.resolve("kakao", Map.of("id", 123L));

        assertThat(result).isInstanceOf(KakaoOAuth2User.class);
        assertThat(result.getProvider()).isEqualTo(AuthProvider.KAKAO);
    }

    @Test
    @DisplayName("naver → NaverOAuth2User를 반환한다")
    void naver_returnsNaverUser() {
        OAuth2UserInfo result = resolver.resolve("naver",
                Map.of("response", Map.of("id", "123")));

        assertThat(result).isInstanceOf(NaverOAuth2User.class);
        assertThat(result.getProvider()).isEqualTo(AuthProvider.NAVER);
    }

    @Test
    @DisplayName("google → GoogleOAuth2User를 반환한다")
    void google_returnsGoogleUser() {
        OAuth2UserInfo result = resolver.resolve("google", Map.of("sub", "123"));

        assertThat(result).isInstanceOf(GoogleOAuth2User.class);
        assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    @DisplayName("KAKAO (대문자) → 대소문자 구분 없이 매칭된다")
    void caseInsensitive_matchesProvider() {
        OAuth2UserInfo result = resolver.resolve("KAKAO", Map.of("id", 123L));

        assertThat(result).isInstanceOf(KakaoOAuth2User.class);
    }

    @Test
    @DisplayName("미지원 Provider → CustomException 발생")
    void unsupportedProvider_throwsException() {
        assertThatThrownBy(() -> resolver.resolve("facebook", Map.of()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("LOCAL → adapterMap에 없으므로 CustomException 발생")
    void local_throwsException() {
        assertThatThrownBy(() -> resolver.resolve("local", Map.of()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("어댑터 1개만 등록: 해당 Provider만 resolve 가능")
    void singleAdapter_onlyThatProviderWorks() {
        OAuth2UserResolver singleResolver = new OAuth2UserResolver(
                List.of(new GoogleOAuth2UserAdapter()));

        OAuth2UserInfo result = singleResolver.resolve("google", Map.of("sub", "123"));
        assertThat(result.getProvider()).isEqualTo(AuthProvider.GOOGLE);

        assertThatThrownBy(() -> singleResolver.resolve("kakao", Map.of("id", 123L)))
                .isInstanceOf(CustomException.class);
    }

}