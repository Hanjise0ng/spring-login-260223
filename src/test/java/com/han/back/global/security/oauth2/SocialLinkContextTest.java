package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.infra.redis.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocialLinkContext")
class SocialLinkContextTest {

    @Mock private RedisUtil redisUtil;

    @InjectMocks private SocialLinkContext socialLinkContext;

    private static final String STATE = "state-abc-123";
    private static final String KEY = OAuth2Const.SOCIAL_LINK_CONTEXT_PREFIX + STATE;

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("state를 키로 userId를 TTL과 함께 저장한다")
        void save_storesWithTtl() {
            socialLinkContext.save(STATE, 4L);

            verify(redisUtil).setDataExpire(eq(KEY), eq("4"), eq(OAuth2Const.SOCIAL_LINK_TOKEN_TTL));
        }
    }

    @Nested
    @DisplayName("consume()")
    class Consume {

        @Test
        @DisplayName("state로 userId를 조회한다")
        void consume_returnsUserId() {
            given(redisUtil.getAndDelete(KEY)).willReturn(Optional.of("4"));

            Optional<Long> result = socialLinkContext.consume(STATE);

            assertThat(result).contains(4L);
        }

        @Test
        @DisplayName("getAndDelete를 사용해 일회성으로 소비한다 (replay 방지)")
        void consume_isOneTime() {
            given(redisUtil.getAndDelete(KEY)).willReturn(Optional.of("4"));

            socialLinkContext.consume(STATE);

            // getAndDelete로 조회와 동시에 삭제 → 같은 state 재사용 불가
            verify(redisUtil).getAndDelete(KEY);
        }

        @Test
        @DisplayName("컨텍스트가 없으면(만료/위조) empty")
        void consume_emptyWhenAbsent() {
            given(redisUtil.getAndDelete(KEY)).willReturn(Optional.empty());

            assertThat(socialLinkContext.consume(STATE)).isEmpty();
        }

        @Test
        @DisplayName("state가 null이면 empty")
        void consume_emptyWhenStateNull() {
            assertThat(socialLinkContext.consume(null)).isEmpty();
        }
    }

}