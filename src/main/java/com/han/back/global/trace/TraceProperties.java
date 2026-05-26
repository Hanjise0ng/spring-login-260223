package com.han.back.global.trace;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "trace")
public class TraceProperties {

    private final boolean strictMode;

    public TraceProperties(boolean strictMode) {
        this.strictMode = strictMode;
    }

}