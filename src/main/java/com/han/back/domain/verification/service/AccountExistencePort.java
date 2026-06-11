package com.han.back.domain.verification.service;

public interface AccountExistencePort {

    boolean existsLocalAccountByEmail(String email);

    // boolean existsLocalAccountByPhone(String phone);

}