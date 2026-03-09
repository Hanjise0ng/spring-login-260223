package com.han.back.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum ClientType {
    WEB("WEB"),
    APP("APP");

    private final String value;

    public static ClientType fromHeader(String headerValue) {
        if (!StringUtils.hasText(headerValue)) {
            return WEB;
        }

        return Arrays.stream(values())
                .filter(type -> type.getValue().equalsIgnoreCase(headerValue))
                .findFirst()
                .orElse(WEB);
    }

}