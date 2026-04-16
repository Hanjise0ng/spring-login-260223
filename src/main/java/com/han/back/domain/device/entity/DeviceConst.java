package com.han.back.domain.device.entity;

import java.util.Set;

public final class DeviceConst {

    private DeviceConst() {}

    public static final int MAX_SESSIONS_PER_USER = 2;

    public static final String FALLBACK_VALUE = "알 수 없음";

    public static final Set<String> TABLET_FAMILIES =
            Set.of("ipad", "kindle", "kindle fire", "nexus 10", "galaxy tab");
    public static final Set<String> MOBILE_OS_FAMILIES =
            Set.of("iOS", "Android");
    public static final Set<String> DESKTOP_OS_FAMILIES =
            Set.of("Windows", "Mac OS X", "Linux", "Chrome OS", "Ubuntu", "Fedora");
    public static final String DESKTOP_DEVICE_FAMILY = "Other";

    public static final String OS_IOS = "iOS";
    public static final String OS_ANDROID = "Android";

}