package com.han.back.global.security.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("CachedBodyRequestWrapper")
class CachedBodyRequestWrapperTest {

    private static final String JSON_BODY = "{\"loginId\":\"testuser\",\"password\":\"Test1234!\"}";
    private static final byte[] BODY = JSON_BODY.getBytes(StandardCharsets.UTF_8);

    private static CachedBodyRequestWrapper wrap(byte[] body) throws IOException {
        MockHttpServletRequest original = new MockHttpServletRequest();
        original.setContent(body);
        return new CachedBodyRequestWrapper(original);
    }

    @Nested
    @DisplayName("본문 재읽기")
    class ReReadable {

        // LoginRateLimitFilter가 loginId를 꺼낸 뒤에도 LoginFilter가 같은 본문을 읽어야 하는 이유로 존재하는 계약
        @Test
        @DisplayName("getCachedBody로 먼저 읽어도 getInputStream으로 같은 본문을 다시 읽을 수 있다")
        void cachedBodyThenInputStream_returnsSameBody() throws IOException {
            CachedBodyRequestWrapper wrapper = wrap(BODY);

            assertThat(wrapper.getCachedBody()).isEqualTo(BODY);
            assertThat(wrapper.getInputStream().readAllBytes()).isEqualTo(BODY);
        }

        @Test
        @DisplayName("getInputStream은 호출마다 처음부터 읽는 새 스트림을 반환한다")
        void inputStream_isFreshOnEveryCall() throws IOException {
            CachedBodyRequestWrapper wrapper = wrap(BODY);

            assertThat(wrapper.getInputStream().readAllBytes()).isEqualTo(BODY);
            assertThat(wrapper.getInputStream().readAllBytes()).isEqualTo(BODY);
        }

        @Test
        @DisplayName("getReader로도 같은 본문을 읽을 수 있다")
        void reader_returnsSameBody() throws IOException {
            CachedBodyRequestWrapper wrapper = wrap(BODY);

            String read = wrapper.getReader().lines().collect(Collectors.joining());

            assertThat(read).isEqualTo(JSON_BODY);
        }
    }

    @Nested
    @DisplayName("ServletInputStream 계약")
    class ServletInputStreamContract {

        @Test
        @DisplayName("isFinished는 끝까지 읽기 전에는 false, 다 읽은 뒤에는 true이며 이후 read는 EOF(-1)이다")
        void isFinished_reflectsConsumption() throws IOException {
            ServletInputStream in = wrap(BODY).getInputStream();

            assertThat(in.isFinished()).isFalse();

            in.readAllBytes();

            assertThat(in.isFinished()).isTrue();
            assertThat(in.read()).isEqualTo(-1);
        }

        @Test
        @DisplayName("빈 본문이면 읽기 전부터 isFinished가 true이다")
        void emptyBody_isFinishedImmediately() throws IOException {
            ServletInputStream in = wrap(new byte[0]).getInputStream();

            assertThat(in.isFinished()).isTrue();
            assertThat(in.read()).isEqualTo(-1);
        }

        @Test
        @DisplayName("메모리 버퍼이므로 isReady는 항상 true이다")
        void isReady_alwaysTrue() throws IOException {
            assertThat(wrap(BODY).getInputStream().isReady()).isTrue();
        }

        @Test
        @DisplayName("비동기 읽기(ReadListener 등록)는 명시적으로 거부한다")
        void setReadListener_isUnsupported() throws IOException {
            ServletInputStream in = wrap(BODY).getInputStream();

            assertThatThrownBy(() -> in.setReadListener(mock(ReadListener.class)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

}
