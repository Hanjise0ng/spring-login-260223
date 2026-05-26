package com.han.back.global.trace;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceInitializer {

    private final TraceProperties traceProperties;

    @PostConstruct
    void init() {
        TraceContext.configureStrictMode(traceProperties.isStrictMode());
        log.info("TraceContext strictMode={}", traceProperties.isStrictMode());
    }

}