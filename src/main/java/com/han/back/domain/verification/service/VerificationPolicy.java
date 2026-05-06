package com.han.back.domain.verification.service;

import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.global.infra.notification.model.NotificationChannel;

import java.util.Set;

public interface VerificationPolicy {

    Set<VerificationType> getSupportedTypes();

    void check(String target, NotificationChannel channel);

}