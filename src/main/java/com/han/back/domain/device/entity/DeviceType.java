package com.han.back.domain.device.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 디바이스 타입 분류.

 * 분류 기준:
 * - WEB_* : 웹 브라우저를 통한 접근 (ClientType.WEB)
 * - APP_* : 네이티브 앱을 통한 접근 (ClientType.APP)
 * - UNKNOWN: 식별 불가

 * uap-java device.family 값 기준:
 * - "Other" → 데스크탑 브라우저 (uap-core 공식 스펙의 기본값)
 * - iPad, Kindle 등 → 태블릿
 * - os.family = iOS / Android → 모바일 브라우저
 */
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

    public boolean isApp() {
        return this == APP_ANDROID || this == APP_IOS;
    }

    public boolean isWeb() {
        return this == WEB_DESKTOP || this == WEB_MOBILE || this == WEB_TABLET;
    }

}