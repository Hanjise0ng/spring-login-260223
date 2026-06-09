package com.han.back.domain.verification.policy;

import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.domain.verification.service.UserExistencePort;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExistingTargetPolicy implements VerificationPolicy {

    private final UserExistencePort userExistencePort;

    @Override
    public Set<VerificationType> getSupportedTypes() {
        return Set.of(VerificationType.PASSWORD_RESET);
    }

    @Override
    public void check(String target, NotificationChannel channel) {
        switch (channel) {
            case EMAIL -> {
                if (!userExistencePort.existsByEmail(target)) {
                    throw new CustomException(AccountResponseStatus.ACCOUNT_USER_NOT_FOUND);
                }
            }
            case SMS -> throw new CustomException(VerificationResponseStatus.VERIFY_CHANNEL_UNSUPPORTED);
        }
    }

}