package com.han.back.domain.auth.factory;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("UserFactory")
class UserFactoryTest {

    private UserFactory userFactory;

    private static final String ENCODED_PASSWORD = "encodedPassword123!";
    private static final String TAG = "A1B2";
    private static final String NICKNAME = "홍길동";
    private static final String EMAIL = "test@example.com";
    private static final String LOGIN_ID = "testUser";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        userFactory = new UserFactory();
    }

    @Nested
    @DisplayName("createLocalUser()")
    class CreateLocalUser {

        private SignUpRequestDto dto;

        @BeforeEach
        void setUpDto() {
            dto = mock(SignUpRequestDto.class);
            given(dto.getEmail()).willReturn(EMAIL);
            given(dto.getNickname()).willReturn(NICKNAME);
        }

        @Test
        @DisplayName("dto의 email·nickname과 tag가 UserEntity에 매핑된다")
        void dtoFieldsMappedToUserEntity() {
            UserEntity user = userFactory.createLocalUser(dto, TAG);

            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getTag()).isEqualTo(TAG);
        }

        @Test
        @DisplayName("role은 항상 USER, authProvider는 항상 LOCAL이다")
        void roleAndProviderAreFixed() {
            UserEntity user = userFactory.createLocalUser(dto, TAG);

            assertThat(user.getRole()).isEqualTo(Role.USER);
            assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        }
    }

    @Nested
    @DisplayName("createLocalCredential()")
    class CreateLocalCredential {

        @Test
        @DisplayName("userId·identifier·encodedPassword가 LOCAL credential에 매핑된다")
        void fieldsMappedToCredential() {
            CredentialEntity credential =
                    userFactory.createLocalCredential(USER_ID, LOGIN_ID, ENCODED_PASSWORD);

            assertThat(credential.getUserId()).isEqualTo(USER_ID);
            assertThat(credential.getIdentifier()).isEqualTo(LOGIN_ID);
            assertThat(credential.getPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(credential.getProvider()).isEqualTo(AuthProvider.LOCAL);
        }
    }

    @Nested
    @DisplayName("createSocialUser()")
    class CreateSocialUser {

        @Test
        @DisplayName("nickname·email·tag가 UserEntity에 매핑된다")
        void fieldsMappedToUserEntity() {
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, AuthProvider.GOOGLE, TAG);

            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getTag()).isEqualTo(TAG);
        }

        @Test
        @DisplayName("role은 항상 USER, authProvider는 인자로 받은 provider다")
        void roleFixedProviderMatchesGiven() {
            UserEntity googleUser = userFactory.createSocialUser(NICKNAME, EMAIL, AuthProvider.GOOGLE, TAG);
            UserEntity kakaoUser = userFactory.createSocialUser(NICKNAME, EMAIL, AuthProvider.KAKAO, TAG);

            assertThat(googleUser.getRole()).isEqualTo(Role.USER);
            assertThat(googleUser.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
            assertThat(kakaoUser.getAuthProvider()).isEqualTo(AuthProvider.KAKAO);
        }

        @Test
        @DisplayName("publicId는 null이 아니며 호출마다 다르다")
        void publicIdIsNotNullAndUnique() {
            UserEntity user1 = userFactory.createSocialUser(NICKNAME, EMAIL, AuthProvider.GOOGLE, TAG);
            UserEntity user2 = userFactory.createSocialUser(NICKNAME, EMAIL, AuthProvider.GOOGLE, TAG);

            assertThat(user1.getPublicId()).isNotNull();
            assertThat(user1.getPublicId()).isNotEqualTo(user2.getPublicId());
        }
    }

}