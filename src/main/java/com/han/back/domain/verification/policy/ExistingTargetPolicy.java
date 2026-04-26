package com.han.back.domain.verification.policy;

import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.UserExistenceChecker;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class ExistingTargetPolicy implements VerificationPolicy {

    private final UserExistenceChecker userExistenceChecker;

    @Override
    public Set<VerificationType> getSupportedTypes() {
        return Set.of(VerificationType.PASSWORD_RESET);
    }

    @Override
    public void check(String target, NotificationChannel channel) {
        switch (channel) {
            case EMAIL -> {
                if (!userExistenceChecker.existsByEmail(target)) {
                    throw new CustomException(BaseResponseStatus.NOT_FOUND_USER);
                }
            }
            case SMS -> throw new CustomException(BaseResponseStatus.UNSUPPORTED_NOTIFICATION_CHANNEL);
        }
    }

}