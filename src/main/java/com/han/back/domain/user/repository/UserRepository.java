package com.han.back.domain.user.repository;

import com.han.back.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    boolean existsByEmail(String email);

}