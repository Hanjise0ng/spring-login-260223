package com.han.back.global.trace;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "trace")
public class TraceProperties {
    private boolean strictMode = false;
}