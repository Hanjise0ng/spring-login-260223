package com.han.back.global.trace;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public final class TraceContext {

    private TraceContext() {}

    private static final String KEY = "traceId";
    private static final String UNTRACED = "UNTRACED";

    private static boolean strictMode = false;

    static void configureStrictMode(boolean strict) {
        strictMode = strict;
    }

    public static String getTraceId() {
        String id = MDC.get(KEY);
        if (id != null) {
            return id;
        }
        if (strictMode) {
            log.warn("traceId가 MDC에 존재하지 않습니다. "
                    + "LoggingFilter 등록 여부와 MdcTaskDecorator 설정을 확인하세요.");
        }
        return UNTRACED;
    }

    public static void setTraceId(String traceId) {
        MDC.put(KEY, traceId);
    }

    public static void clearTraceId() {
        MDC.remove(KEY);
    }

    public static boolean isTraced() {
        return MDC.get(KEY) != null;
    }

}