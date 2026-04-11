package com.han.back.domain.auth.service.implement;

import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DuplicateEmailPolicy implements VerificationPolicy {

    private final UserRepository userRepository;

    @Override
    public Set<VerificationType> getSupportedTypes() {
        return Set.of(VerificationType.SIGN_UP, VerificationType.EMAIL_CHANGE);
    }

    @Override
    public void check(String target) {
        if (userRepository.existsByEmail(target)) {
            throw new CustomException(BaseResponseStatus.DUPLICATE_EMAIL);
        }
    }

}