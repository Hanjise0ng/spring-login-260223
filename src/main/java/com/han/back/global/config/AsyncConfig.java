package com.han.back.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);                                     // 기본 스레드 수 (메일 발송 빈도 고려)
        executor.setMaxPoolSize(10);                                     // 큐가 가득 찰 경우 허용할 최대 스레드 수
        executor.setQueueCapacity(500);                                  // 작업 대기 큐 크기
        executor.setThreadNamePrefix("notification-");                   // 디버깅을 위한 스레드 이름 접두사
        executor.setKeepAliveSeconds(60);                                // 초과 생성된 스레드의 유휴 생존 시간 (60초 후 회수)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 큐 포화 시 호출자 스레드에서 직접 실행 (유실 방지)
        executor.setWaitForTasksToCompleteOnShutdown(true);              // 서버 종료 시 잔여 작업 처리 보장 (Graceful Shutdown)
        executor.setAwaitTerminationSeconds(30);                         // 작업 완료 최대 대기 시간 (30초 초과 시 강제 종료)
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> log.error(
                "Async uncaught exception - method: {} | args: {} | error: {}",
                method.getName(), Arrays.toString(params), ex.getMessage(), ex);
    }

}