package com.han.back.domain.auth.attempt.repository;

import com.han.back.domain.auth.attempt.entity.LoginAttemptEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {

    Page<LoginAttemptEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

}