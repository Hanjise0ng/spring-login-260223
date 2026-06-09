package com.han.back.domain.user.service;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TagGenerator")
class TagGeneratorTest {

    @Mock private UserRepository userRepository;
    @Mock private SecureRandom secureRandom;

    private TagGenerator tagGenerator;

    private static final String NICKNAME = "홍길동";
    private static final String OTHER_NICKNAME = "이순신";
    private static final String FIXED_TAG = "A1B2";

    @BeforeEach
    void setUp() {
        tagGenerator = new TagGenerator(userRepository, secureRandom);
    }

    private void injectRandom(int... values) throws Exception {
        SecureRandom stub = new SecureRandom() {
            private int idx = 0;

            @Override
            public int nextInt(int bound) {
                return values[idx++];
            }
        };
        Field f = TagGenerator.class.getDeclaredField("secureRandom");
        f.setAccessible(true);
        f.set(tagGenerator, stub);
    }

    /** 지정 닉네임 + 어떤 태그도 중복 없음 */
    private void stubNoDuplicate(String nickname) {
        given(userRepository.existsByNicknameAndTag(eq(nickname), anyString())).willReturn(false);
    }

    /** 지정 닉네임 + 어떤 태그도 항상 중복 */
    private void stubAlwaysDuplicate(String nickname) {
        given(userRepository.existsByNicknameAndTag(eq(nickname), anyString())).willReturn(true);
    }

    @Nested
    @DisplayName("generate()")
    class Generate {

        @Test
        @DisplayName("첫 시도 성공 → 태그 반환 + existsByNicknameAndTag 1회 호출")
        void firstAttemptSucceeds() {
            given(secureRandom.nextInt(0x10000)).willReturn(0xA1B2);
            given(userRepository.existsByNicknameAndTag(eq(NICKNAME), anyString())).willReturn(false);

            String tag = tagGenerator.generate(NICKNAME);

            assertThat(tag).isEqualTo("A1B2");
            then(userRepository).should(times(1)).existsByNicknameAndTag(NICKNAME, "A1B2");
        }

        @Test
        @DisplayName("2회 충돌 후 3번째 성공 → 3번째 태그 반환 + 3회 호출")
        void twoCollisionsThenSuccess_returnsThirdTagAndCallsThreeTimes() throws Exception {
            injectRandom(0x1111, 0x2222, 0x3333);
            given(userRepository.existsByNicknameAndTag(NICKNAME, "1111")).willReturn(true);
            given(userRepository.existsByNicknameAndTag(NICKNAME, "2222")).willReturn(true);
            given(userRepository.existsByNicknameAndTag(NICKNAME, "3333")).willReturn(false);

            String tag = tagGenerator.generate(NICKNAME);

            assertThat(tag).isEqualTo("3333");
            then(userRepository).should(times(3))
                    .existsByNicknameAndTag(eq(NICKNAME), anyString());
        }

        @Test
        @DisplayName("TAG_GENERATION_RETRY 횟수만큼 모두 충돌 → CustomException(TAG_GENERATION_FAILED)")
        void allAttemptsFail_throwsTagGenerationFailed() {
            stubAlwaysDuplicate("인기닉네임");

            assertThatThrownBy(() -> tagGenerator.generate("인기닉네임"))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(AccountResponseStatus.ACCOUNT_TAG_GENERATION_FAIL);

            then(userRepository).should(times(OAuth2Const.TAG_GENERATION_RETRY))
                    .existsByNicknameAndTag(eq("인기닉네임"), anyString());
        }

        @Test
        @DisplayName("닉네임이 다르면 같은 태그값도 허용 → 타 닉네임은 DB 조회 없이 성공")
        void differentNickname_sameTagAllowed_queriesOnlyTargetNickname() throws Exception {
            injectRandom(0xA1B2);
            // "홍길동"+"A1B2"는 이미 사용 중이지만, 조회 대상은 "이순신"
            given(userRepository.existsByNicknameAndTag(OTHER_NICKNAME, FIXED_TAG)).willReturn(false);

            String tag = tagGenerator.generate(OTHER_NICKNAME);

            assertThat(tag).isEqualTo(FIXED_TAG);
            then(userRepository).should(never())
                    .existsByNicknameAndTag(eq(NICKNAME), anyString());
            then(userRepository).should(times(1))
                    .existsByNicknameAndTag(OTHER_NICKNAME, FIXED_TAG);
        }

        @Test
        @DisplayName("반환 태그는 정확히 4자리 대문자 16진수 포맷(%04X)")
        void returnedTag_isUppercaseHexPaddedToFourDigits() throws Exception {
            injectRandom(0x000F); // 패딩 필요한 값
            stubNoDuplicate(NICKNAME);

            String tag = tagGenerator.generate(NICKNAME);

            assertThat(tag).isEqualTo("000F");
        }
    }

}