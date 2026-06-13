package com.han.back.domain.user.policy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class WithdrawalGracePolicy {

    public static final Duration GRACE_PERIOD = Duration.ofDays(30);

    private WithdrawalGracePolicy() {}

    public static LocalDateTime expiresAt(LocalDateTime deletedAt) {
        return deletedAt.plus(GRACE_PERIOD);
    }

    public static boolean isRecoverable(LocalDateTime deletedAt, LocalDateTime now) {
        return now.isBefore(expiresAt(deletedAt));
    }

    public static long remainingDays(LocalDateTime deletedAt, LocalDateTime now) {
        long days = ChronoUnit.DAYS.between(now.toLocalDate(), expiresAt(deletedAt).toLocalDate());
        return Math.max(days, 0);
    }

}