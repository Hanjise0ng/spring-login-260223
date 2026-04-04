package com.han.back.domain.verification.service;

import com.han.back.domain.verification.entity.VerificationType;

import java.util.Set;

public interface VerificationPolicy {

    Set<VerificationType> getSupportedTypes();

    void validate(String target);

}