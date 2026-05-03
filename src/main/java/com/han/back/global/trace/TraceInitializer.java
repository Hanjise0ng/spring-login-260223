package com.han.back.global.trace;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(TraceProperties.class)
public class TraceInitializer {

    private final TraceProperties traceProperties;

    @PostConstruct
    void init() {
        TraceContext.configureStrictMode(traceProperties.isStrictMode());
        log.info("TraceContext strictMode={}", traceProperties.isStrictMode());
    }

}