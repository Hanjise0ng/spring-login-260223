package com.han.back.domain.verification.entity;

import java.time.Duration;

public final class VerificationConst {

    private VerificationConst() {}

    // Duration으로 정의 — 단위가 타입에 내재됨
    public static final Duration CODE_TTL      = Duration.ofMinutes(5);
    public static final Duration COOLDOWN_TTL  = Duration.ofMinutes(1);
    public static final Duration CONFIRMED_TTL = Duration.ofMinutes(30);

    public static final int CODE_LENGTH = 6;
    public static final int CODE_BOUND  = 1_000_000;

    // 파생값 — Duration에서 직접 추출, 수동 나눗셈 제거
    public static long codeTtlMinutes() {
        return CODE_TTL.toMinutes();
    }

    private static final String CODE_PREFIX      = "verification:code:";
    private static final String COOLDOWN_PREFIX  = "verification:cooldown:";
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