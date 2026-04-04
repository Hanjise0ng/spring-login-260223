package com.han.back.domain.verification.entity;

public final class VerificationConst {

    private VerificationConst() {}

    public static final long CODE_TTL = 300_000L;
    public static final long COOLDOWN_TTL = 60_000L;
    public static final long CONFIRMED_TTL = 1_800_000L;

    public static final int CODE_LENGTH = 6;
    public static final int CODE_BOUND = 1_000_000;

    public static final long CODE_TTL_MINUTES = CODE_TTL / 60_000;

    private static final String CODE_PREFIX = "verification:code:";
    private static final String COOLDOWN_PREFIX = "verification:cooldown:";
    private static final String CONFIRMED_PREFIX = "verification:confirmed:";

    public static String codeKey(VerificationType type, String target) {
        return CODE_PREFIX + type.name() + ":" + target;
    }

    public static String cooldownKey(VerificationType type, String target) {
        return COOLDOWN_PREFIX + type.name() + ":" + target;
    }

    public static String confirmedKey(VerificationType type, String target) {
        return CONFIRMED_PREFIX + type.name() + ":" + target;
    }

}