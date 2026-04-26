package com.han.back.domain.verification.service;

public interface UserExistenceChecker {

    boolean existsByEmail(String email);

    // boolean existsByPhone(String phone);

}