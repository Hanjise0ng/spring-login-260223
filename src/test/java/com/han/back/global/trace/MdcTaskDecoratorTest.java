package com.han.back.global.trace;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("MdcTaskDecorator")
class MdcTaskDecoratorTest {

    private MdcTaskDecorator decorator;

    @BeforeEach
    void setUp() {
        decorator = new MdcTaskDecorator();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("MDC 전파")
    class MdcPropagation {

        @Test
        @DisplayName("호출 스레드의 MDC가 데코레이트된 Runnable 실행 시점에 전파된다")
        void mdcPropagated_toDecoratedRunnable() throws Exception {
            // given - 호출 스레드(부모)에 MDC 설정
            MDC.put("traceId", "parent-trace-xyz");
            AtomicReference<String> capturedTraceId = new AtomicReference<>();

            Runnable decorated = decorator.decorate(
                    () -> capturedTraceId.set(MDC.get("traceId"))
            );

            // MDC를 캡처한 이후 실행 전에 호출 스레드 MDC를 지워도 전파돼야 함
            MDC.clear();

            // when - 다른 스레드에서 실행 (실제 비동기 환경 시뮬레이션)
            CompletableFuture.runAsync(decorated).get(2, TimeUnit.SECONDS);

            // then
            assertThat(capturedTraceId.get()).isEqualTo("parent-trace-xyz");
        }

        @Test
        @DisplayName("여러 MDC 키가 모두 전파된다")
        void allMdcEntries_arePropagated() throws Exception {
            MDC.put("traceId", "trace-001");
            MDC.put("loginId", "user-hong");
            AtomicReference<String> capturedLoginId = new AtomicReference<>();
            AtomicReference<String> capturedTraceId = new AtomicReference<>();

            Runnable decorated = decorator.decorate(() -> {
                capturedTraceId.set(MDC.get("traceId"));
                capturedLoginId.set(MDC.get("loginId"));
            });

            CompletableFuture.runAsync(decorated).get(2, TimeUnit.SECONDS);

            assertThat(capturedTraceId.get()).isEqualTo("trace-001");
            assertThat(capturedLoginId.get()).isEqualTo("user-hong");
        }

        @Test
        @DisplayName("MDC가 비어있으면 데코레이트된 Runnable은 빈 MDC로 실행된다")
        void emptyMdc_runnableRunsWithEmptyMdc() throws Exception {
            // MDC.getCopyOfContextMap()이 null을 반환하는 경우 → null 체크 분기
            // MDC가 빈 상태에서도 예외 없이 실행돼야 함
            AtomicReference<String> capturedTraceId = new AtomicReference<>("SENTINEL");

            Runnable decorated = decorator.decorate(
                    () -> capturedTraceId.set(MDC.get("traceId"))
            );

            CompletableFuture.runAsync(decorated).get(2, TimeUnit.SECONDS);

            // MDC에 없으므로 null
            assertThat(capturedTraceId.get()).isNull();
        }
    }

    @Nested
    @DisplayName("MDC 정리")
    class MdcCleanup {

        @Test
        @DisplayName("Runnable 실행 후 해당 스레드의 MDC가 비워진다")
        void afterRunnable_mdcIsCleared() throws Exception {
            MDC.put("traceId", "cleanup-trace");
            AtomicReference<String> mdcAfterRun = new AtomicReference<>("NOT_CLEARED");

            Runnable task = () -> {
                // 실행 완료 후 finally에서 MDC.clear() 호출 예정
            };
            Runnable decorated = decorator.decorate(task);

            CompletableFuture.runAsync(() -> {
                decorated.run();
                // finally 블록 실행 후 MDC 상태를 캡처
                mdcAfterRun.set(MDC.get("traceId"));
            }).get(2, TimeUnit.SECONDS);

            assertThat(mdcAfterRun.get()).isNull();
        }

        @Test
        @DisplayName("Runnable에서 예외가 발생해도 MDC가 비워진다 (finally 보장)")
        void onRunnableException_mdcIsStillCleared() throws Exception {
            MDC.put("traceId", "exception-trace");
            AtomicReference<String> mdcAfterException = new AtomicReference<>("NOT_CLEARED");

            Runnable failingTask = () -> { throw new RuntimeException("task failed"); };
            Runnable decorated = decorator.decorate(failingTask);

            CompletableFuture.runAsync(() -> {
                try {
                    decorated.run();
                } catch (RuntimeException ignored) {
                    // 예외 발생 후 MDC 상태 캡처
                }
                mdcAfterException.set(MDC.get("traceId"));
            }).get(2, TimeUnit.SECONDS);

            assertThat(mdcAfterException.get()).isNull();
        }
    }

    @Nested
    @DisplayName("Runnable 실행")
    class RunnableExecution {

        @Test
        @DisplayName("원본 Runnable이 반드시 실행된다")
        void originalRunnable_isExecuted() throws Exception {
            AtomicReference<Boolean> executed = new AtomicReference<>(false);

            Runnable decorated = decorator.decorate(() -> executed.set(true));
            CompletableFuture.runAsync(decorated).get(2, TimeUnit.SECONDS);

            assertThat(executed.get()).isTrue();
        }

        @Test
        @DisplayName("decorate()는 예외 없이 Runnable을 반환한다")
        void decorate_returnsRunnableWithoutException() {
            assertThatCode(() -> decorator.decorate(() -> {}))
                    .doesNotThrowAnyException();
        }
    }

}