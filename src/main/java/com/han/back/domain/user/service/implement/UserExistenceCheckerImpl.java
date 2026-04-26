package com.han.back.domain.user.service.implement;

import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.service.UserExistenceChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserExistenceCheckerImpl implements UserExistenceChecker {

    private final UserRepository userRepository;

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

//    @Override
//    public boolean existsByPhone(String phone) {
//        return userRepository.existsByPhone(phone);
//    }

}