package com.han.back.domain.auth.factory;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserFactory")
class UserFactoryTest {

    private UserFactory userFactory;

    private static final String ENCODED_PASSWORD = "encodedPassword123!";
    private static final String TAG = "A1B2";
    private static final String NICKNAME = "홍길동";
    private static final String EMAIL = "test@example.com";
    private static final String LOGIN_ID = "testUser";

    @BeforeEach
    void setUp() {
        userFactory = new UserFactory();
    }

    @Nested
    @DisplayName("createFromSignUpRequest()")
    class CreateFromSignUpRequest {

        private SignUpRequestDto dto;

        @BeforeEach
        void setUpDto() {
            dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getEmail()).willReturn(EMAIL);
            given(dto.getNickname()).willReturn(NICKNAME);
        }

        @Test
        @DisplayName("dto 필드가 UserEntity에 올바르게 매핑된다")
        void dtoFields_mappedToUserEntity() {
            UserEntity user = userFactory.createFromSignUpRequest(dto, ENCODED_PASSWORD, TAG);

            assertThat(user.getLoginId()).isEqualTo(LOGIN_ID);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
        }

        @Test
        @DisplayName("encodedPassword가 UserEntity의 password에 설정된다")
        void encodedPassword_setToUserEntity() {
            UserEntity user = userFactory.createFromSignUpRequest(dto, ENCODED_PASSWORD, TAG);

            assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
        }

        @Test
        @DisplayName("tag가 UserEntity의 tag에 설정된다")
        void tag_setToUserEntity() {
            UserEntity user = userFactory.createFromSignUpRequest(dto, ENCODED_PASSWORD, TAG);

            assertThat(user.getTag()).isEqualTo(TAG);
        }

        @Test
        @DisplayName("role은 항상 USER로 설정된다")
        void role_isAlwaysUser() {
            UserEntity user = userFactory.createFromSignUpRequest(dto, ENCODED_PASSWORD, TAG);

            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("authProvider는 항상 LOCAL로 설정된다")
        void authProvider_isAlwaysLocal() {
            UserEntity user = userFactory.createFromSignUpRequest(dto, ENCODED_PASSWORD, TAG);

            assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        }
    }

    @Nested
    @DisplayName("createSocialUser()")
    class CreateSocialUser {

        @Test
        @DisplayName("nickname, email, tag가 UserEntity에 올바르게 매핑된다")
        void fields_mappedToUserEntity() {
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);

            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getTag()).isEqualTo(TAG);
        }

        @Test
        @DisplayName("role은 항상 USER로 설정된다")
        void role_isAlwaysUser() {
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);

            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("authProvider가 인자로 받은 provider로 설정된다")
        void authProvider_matchesGivenProvider() {
            UserEntity googleUser = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);
            UserEntity kakaoUser = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.KAKAO, TAG);

            assertThat(googleUser.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(kakaoUser.getAuthProvider()).isEqualTo(AuthProvider.KAKAO);
        }

        @Test
        @DisplayName("loginId는 provider 값과 publicId 앞 8자리 대문자를 포함한 더미 포맷이다")
        void loginId_matchesDummyFormat() {
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);

            // provider value가 포함되어야 한다
            assertThat(user.getLoginId()).contains(AuthProvider.GOOGLE.getValue());
        }

        @Test
        @DisplayName("publicId는 null이 아니다")
        void publicId_isNotNull() {
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);

            assertThat(user.getPublicId()).isNotNull();
        }

        @Test
        @DisplayName("두 번 호출 시 서로 다른 publicId가 생성된다")
        void twoCallsProduceDifferentPublicIds() {
            UserEntity user1 = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);
            UserEntity user2 = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);

            assertThat(user1.getPublicId()).isNotEqualTo(user2.getPublicId());
        }

        @Test
        @DisplayName("loginId는 provider 값과 publicId 기반 더미 포맷이다")
        void loginId_containsProviderAndFollowsDummyFormat() {
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, ENCODED_PASSWORD, AuthProvider.GOOGLE, TAG);

            // "GOOGLE_XXXXXXXX" 형식 검증
            assertThat(user.getLoginId())
                    .startsWith(AuthProvider.GOOGLE.getValue() + "_")
                    .matches(AuthProvider.GOOGLE.getValue() + "_[0-9A-F]{8}");
        }
    }

}