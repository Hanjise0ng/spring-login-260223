package com.han.back.global.security.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ClientType {
    WEB("WEB"),
    APP("APP");

    private final String value;

    public static ClientType fromHeader(String headerValue) {
        if ("APP".equalsIgnoreCase(headerValue)) return APP;
        return WEB;
    }

}