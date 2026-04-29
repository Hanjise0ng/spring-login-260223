package com.han.back.domain.verification.service;

public interface UserExistencePort {

    boolean existsByEmail(String email);

    // boolean existsByPhone(String phone);

}