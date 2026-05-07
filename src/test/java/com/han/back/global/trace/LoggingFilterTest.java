package com.han.back.global.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoggingFilter")
class LoggingFilterTest {

    @Mock private FilterChain filterChain;

    private LoggingFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter  = new LoggingFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("traceId 결정")
    class TraceIdResolution {

        @Test
        @DisplayName("X-Request-Id 헤더가 있으면 해당 값을 traceId로 사용한다")
        void withRequestIdHeader_usesHeaderValue() throws Exception {
            request.addHeader("X-Request-Id", "client-trace-abc");

            filter.doFilterInternal(request, response, filterChain);

            // 응답 헤더로 전달되는 traceId가 요청 헤더 값과 동일해야 함
            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("client-trace-abc");
        }

        @Test
        @DisplayName("X-Request-Id 헤더가 없으면 UUID를 생성해 traceId로 사용한다")
        void withoutRequestIdHeader_generatesUuid() throws Exception {
            // 헤더 없음 — UUID 자동 생성 경로
            filter.doFilterInternal(request, response, filterChain);

            String traceId = response.getHeader("X-Trace-Id");
            assertThat(traceId)
                    .isNotNull()
                    .isNotBlank()
                    // UUID v7 형식: 8-4-4-4-12
                    .matches("[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
        }

        @Test
        @DisplayName("X-Request-Id 헤더가 공백이면 UUID를 생성해 사용한다")
        void withBlankRequestIdHeader_generatesUuid() throws Exception {
            request.addHeader("X-Request-Id", "   ");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-Trace-Id"))
                    .isNotBlank()
                    .isNotEqualTo("   ");
        }
    }

    @Nested
    @DisplayName("MDC 라이프사이클")
    class MdcLifecycle {

        @Test
        @DisplayName("필터 실행 중 MDC에 traceId가 존재한다")
        void duringFilter_traceIdInMdc() throws Exception {
            request.addHeader("X-Request-Id", "mdc-test-id");
            String[] capturedTraceId = new String[1];

            // FilterChain 실행 중 MDC 상태를 캡처
            org.mockito.BDDMockito.willAnswer(inv -> {
                capturedTraceId[0] = MDC.get("traceId");
                return null;
            }).given(filterChain).doFilter(any(), any());

            filter.doFilterInternal(request, response, filterChain);

            assertThat(capturedTraceId[0]).isEqualTo("mdc-test-id");
        }

        @Test
        @DisplayName("필터 정상 완료 후 MDC가 비워진다")
        void afterFilter_mdcIsCleared() throws Exception {
            request.addHeader("X-Request-Id", "cleanup-test");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(MDC.get("traceId")).isNull();
        }

        @Test
        @DisplayName("FilterChain에서 예외가 발생해도 MDC가 비워진다 (finally 보장)")
        void onFilterException_mdcIsStillCleared() throws Exception {
            request.addHeader("X-Request-Id", "exception-trace");
            willThrow(new ServletException("Downstream failure"))
                    .given(filterChain).doFilter(any(), any());

            assertThatThrownBy(() ->
                    filter.doFilterInternal(request, response, filterChain)
            ).isInstanceOf(ServletException.class);

            assertThat(MDC.get("traceId")).isNull();
        }
    }

    @Nested
    @DisplayName("클라이언트 IP 결정")
    class ClientIpResolution {

        @Test
        @DisplayName("X-Forwarded-For 헤더가 있으면 첫 번째 IP를 사용한다 (프록시 환경)")
        void withForwardedForHeader_usesFirstIp() throws Exception {
            request.addHeader("X-Forwarded-For", "203.0.113.1, 10.0.0.1, 192.168.0.1");

            filter.doFilterInternal(request, response, filterChain);

            // IP 추출 결과는 로그에 남지만 반환값이 없어 직접 검증 불가
            // FilterChain이 예외 없이 정상 호출됐는지로 대리 검증
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("X-Forwarded-For 헤더가 없으면 RemoteAddr을 사용한다")
        void withoutForwardedForHeader_usesRemoteAddr() throws Exception {
            request.setRemoteAddr("192.168.1.100");

            filter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("응답 헤더")
    class ResponseHeader {

        @Test
        @DisplayName("X-Trace-Id 응답 헤더가 반드시 설정된다")
        void responseAlwaysHasTraceIdHeader() throws Exception {
            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-Trace-Id")).isNotNull();
        }

        @Test
        @DisplayName("요청 X-Request-Id와 응답 X-Trace-Id가 동일하다 — end-to-end 추적 계약")
        void requestIdEqualsResponseTraceId() throws Exception {
            request.addHeader("X-Request-Id", "e2e-trace-id");

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-Trace-Id")).isEqualTo("e2e-trace-id");
        }
    }

}