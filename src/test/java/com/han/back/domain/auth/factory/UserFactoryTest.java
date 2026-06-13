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

class UserFactoryTest {

    private static final String EMAIL = "user@test.com";
    private static final String NICKNAME = "테스터";
    private static final String TAG = "A1B2";
    private static final String LOGIN_ID = "tester123";
    private static final String ENCODED_PASSWORD = "$2a$10$encoded";
    private static final Long USER_ID = 1L;
    private static final String PROVIDER_ID = "kakao-1234567890";

    private final UserFactory userFactory = new UserFactory();

    @Nested
    @DisplayName("createLocalUser()")
    class CreateLocalUser {

        private SignUpRequestDto dto;

        @BeforeEach
        void setUp() {
            dto = new SignUpRequestDto(LOGIN_ID, "rawPassword1!", EMAIL, NICKNAME, "loginIdToken");
        }

        @Test
        @DisplayName("email·nickname·tag가 UserEntity에 매핑된다")
        void fieldsMappedToUserEntity() {
            UserEntity user = userFactory.createLocalUser(dto, TAG);

            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getTag()).isEqualTo(TAG);
        }

        @Test
        @DisplayName("role은 항상 USER다")
        void roleIsFixed() {
            UserEntity user = userFactory.createLocalUser(dto, TAG);

            assertThat(user.getRole()).isEqualTo(Role.USER);
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
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, TAG);

            assertThat(user.getNickname()).isEqualTo(NICKNAME);
            assertThat(user.getEmail()).isEqualTo(EMAIL);
            assertThat(user.getTag()).isEqualTo(TAG);
        }

        @Test
        @DisplayName("role은 항상 USER다")
        void roleIsFixed() {
            UserEntity user = userFactory.createSocialUser(NICKNAME, EMAIL, TAG);

            assertThat(user.getRole()).isEqualTo(Role.USER);
        }

        @Test
        @DisplayName("publicId는 null이 아니며 호출마다 다르다")
        void publicIdIsNotNullAndUnique() {
            UserEntity user1 = userFactory.createSocialUser(NICKNAME, EMAIL, TAG);
            UserEntity user2 = userFactory.createSocialUser(NICKNAME, EMAIL, TAG);

            assertThat(user1.getPublicId()).isNotNull();
            assertThat(user1.getPublicId()).isNotEqualTo(user2.getPublicId());
        }
    }

    @Nested
    @DisplayName("createSocialCredential()")
    class CreateSocialCredential {

        @Test
        @DisplayName("userId·provider·providerId가 매핑되고 password는 null이다")
        void fieldsMappedAndPasswordIsNull() {
            CredentialEntity credential =
                    userFactory.createSocialCredential(USER_ID, AuthProvider.KAKAO, PROVIDER_ID);

            assertThat(credential.getUserId()).isEqualTo(USER_ID);
            assertThat(credential.getProvider()).isEqualTo(AuthProvider.KAKAO);
            assertThat(credential.getIdentifier()).isEqualTo(PROVIDER_ID);
            assertThat(credential.getPassword()).isNull();
        }

        @Test
        @DisplayName("provider별로 동일한 providerId여도 각각 매핑된다")
        void providerVaries() {
            CredentialEntity kakao =
                    userFactory.createSocialCredential(USER_ID, AuthProvider.KAKAO, PROVIDER_ID);
            CredentialEntity naver =
                    userFactory.createSocialCredential(USER_ID, AuthProvider.NAVER, PROVIDER_ID);

            assertThat(kakao.getProvider()).isEqualTo(AuthProvider.KAKAO);
            assertThat(naver.getProvider()).isEqualTo(AuthProvider.NAVER);
        }
    }

}