package com.han.back.global.util;

public final class MaskingUtil {

    private MaskingUtil() {}

    public static String maskTarget(String target) {
        if (target == null) return "null";
        if (target.contains("@")) {
            int atIndex = target.indexOf("@");
            if (atIndex <= 2) return "***" + target.substring(atIndex);
            return target.substring(0, 2) + "***" + target.substring(atIndex);
        }
        if (target.length() > 4) {
            return target.substring(0, 3) + "****" + target.substring(target.length() - 4);
        }
        return "****";
    }

}