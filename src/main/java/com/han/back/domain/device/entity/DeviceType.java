package com.han.back.domain.device.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeviceType {

    WEB_DESKTOP("PC 브라우저"),
    WEB_MOBILE("모바일 브라우저"),
    WEB_TABLET("태블릿 브라우저"),

    APP_ANDROID("Android 앱"),
    APP_IOS("iOS 앱"),

    UNKNOWN("알 수 없음");

    private final String displayName;

}