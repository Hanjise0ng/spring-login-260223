package com.han.back.domain.auth.attempt.repository;

import com.han.back.domain.auth.attempt.entity.LoginAttemptEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LoginAttemptRepositoryTest {

    @Autowired
    private LoginAttemptRepository loginHistoryRepository;

    @Test
    @DisplayName("사용자 로그인 기록은 최신순으로 조회된다")
    void historyIsOrderedByCreatedAtDesc() {
        loginHistoryRepository.save(LoginAttemptEntity.success(1L, "10.0.0.1", "fp-a"));
        loginHistoryRepository.save(LoginAttemptEntity.failure(1L, "10.0.0.2", "fp-b"));
        loginHistoryRepository.saveAndFlush(LoginAttemptEntity.success(1L, "10.0.0.3", "fp-c"));

        Page<LoginAttemptEntity> page = loginHistoryRepository
                .findAllByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getIp()).isEqualTo("10.0.0.3");
    }

    @Test
    @DisplayName("다른 사용자의 기록은 조회되지 않는다")
    void historyIsScopedByUser() {
        loginHistoryRepository.save(LoginAttemptEntity.success(1L, "10.0.0.1", "fp-a"));
        loginHistoryRepository.saveAndFlush(LoginAttemptEntity.success(2L, "10.0.0.2", "fp-b"));

        Page<LoginAttemptEntity> page = loginHistoryRepository
                .findAllByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo(1L);
    }

}