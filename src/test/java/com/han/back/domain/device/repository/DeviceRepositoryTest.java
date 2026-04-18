package com.han.back.domain.device.repository;

import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.fixture.DeviceFixture;
import com.han.back.fixture.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DeviceRepository")
class DeviceRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired DeviceRepository deviceRepository;

    private Long userId;
    private Long otherUserId;

    @BeforeEach
    void setUp() {
        UserEntity user = em.persistAndFlush(UserFixture.localUser());
        userId = user.getId();
    }

    /** 다른 사용자가 필요한 테스트에서만 호출 — 중복 생성 방지용 헬퍼 */
    private Long persistOtherUser() {
        if (otherUserId == null) {
            UserEntity other = em.persistAndFlush(UserFixture.adminUser());
            otherUserId = other.getId();
        }
        return otherUserId;
    }

    @Nested
    @DisplayName("findByPublicIdAndUserId()")
    class FindByPublicIdAndUserId {

        @Test
        @DisplayName("publicId + userId 일치 → 디바이스를 반환한다")
        void matchingPublicIdAndUserId_returnsDevice() {
            DeviceEntity device = em.persistAndFlush(DeviceFixture.activeWebDevice(userId));

            Optional<DeviceEntity> result =
                    deviceRepository.findByPublicIdAndUserId(device.getPublicId(), userId);

            assertThat(result).isPresent();
            assertThat(result.get().getPublicId()).isEqualTo(device.getPublicId());
        }

        @Test
        @DisplayName("publicId는 일치하지만 userId가 다르면 반환하지 않는다 (IDOR 방어)")
        void publicIdMatches_butUserIdDiffers_returnsEmpty() {
            Long other = persistOtherUser();
            DeviceEntity otherDevice = em.persistAndFlush(DeviceFixture.activeWebDevice(other));

            Optional<DeviceEntity> result =
                    deviceRepository.findByPublicIdAndUserId(otherDevice.getPublicId(), userId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 publicId → Optional.empty()를 반환한다")
        void nonExistentPublicId_returnsEmpty() {
            Optional<DeviceEntity> result =
                    deviceRepository.findByPublicIdAndUserId("non-existent-uuid", userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllByUserIdOrderByLastLoginAtDesc()")
    class FindAllByUserIdOrderByLastLoginAtDesc {

        @Test
        @DisplayName("최근 로그인 순(DESC)으로 반환한다")
        void returnsDevices_orderedByLastLoginAtDesc() {
            persistDevice("fp-oldest", "session-oldest", LocalDateTime.of(2025, 1, 1, 0, 0));
            persistDevice("fp-middle", "session-middle", LocalDateTime.of(2025, 6, 1, 0, 0));
            persistDevice("fp-newest", "session-newest", LocalDateTime.of(2025, 12, 1, 0, 0));

            List<DeviceEntity> result =
                    deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(userId);

            assertThat(result)
                    .extracting(DeviceEntity::getDeviceFingerprint)
                    .containsExactly("fp-newest", "fp-middle", "fp-oldest");
        }

        @Test
        @DisplayName("다른 사용자의 디바이스는 결과에 포함되지 않는다")
        void excludesOtherUsersDevices() {
            Long other = persistOtherUser();

            em.persistAndFlush(DeviceFixture.activeWebDevice(userId));
            em.persistAndFlush(DeviceFixture.builder(other)
                    .fingerprint("fp-other-user")
                    .build());

            List<DeviceEntity> result =
                    deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(userId);

            assertThat(result)
                    .extracting(DeviceEntity::getDeviceFingerprint)
                    .containsExactly(DeviceFixture.DEFAULT_WEB_FINGERPRINT);
        }

        @Test
        @DisplayName("디바이스가 없으면 빈 리스트를 반환한다")
        void noDevices_returnsEmptyList() {
            List<DeviceEntity> result =
                    deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(userId);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveDevicesByUserIdOldestFirst()")
    class FindActiveDevicesByUserIdOldestFirst {

        @Test
        @DisplayName("sessionId IS NOT NULL 인 디바이스만 오래된 순(ASC)으로 반환한다")
        void returnsOnlyActiveDevices_orderedByLastLoginAtAsc() {
            persistDevice("fp-oldest", "session-oldest", LocalDateTime.of(2025, 1, 1, 0, 0));
            persistDevice("fp-newest", "session-newest", LocalDateTime.of(2025, 12, 1, 0, 0));
            persistDevice("fp-inactive", null, LocalDateTime.of(2025, 6, 1, 0, 0));

            List<DeviceEntity> result =
                    deviceRepository.findActiveDevicesByUserIdOldestFirst(userId);

            assertThat(result)
                    .extracting(DeviceEntity::getDeviceFingerprint)
                    .containsExactly("fp-oldest", "fp-newest");
        }

        @Test
        @DisplayName("비활성 디바이스만 있으면 빈 리스트를 반환한다")
        void allInactive_returnsEmptyList() {
            em.persistAndFlush(DeviceFixture.inactiveDevice(userId));

            List<DeviceEntity> result =
                    deviceRepository.findActiveDevicesByUserIdOldestFirst(userId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("최대 세션 정책에서 index 0이 가장 오래된 퇴출 대상임을 보장한다")
        void firstElement_isOldestSession_forEvictionPolicy() {
            persistDevice("fp-first", "oldest-session", LocalDateTime.of(2025, 1, 1, 0, 0));
            persistDevice("fp-second", "middle-session", LocalDateTime.of(2025, 6, 1, 0, 0));
            persistDevice("fp-third", "newest-session", LocalDateTime.of(2025, 12, 1, 0, 0));

            List<DeviceEntity> result =
                    deviceRepository.findActiveDevicesByUserIdOldestFirst(userId);

            assertThat(result)
                    .extracting(DeviceEntity::getSessionId)
                    .containsExactly("oldest-session", "middle-session", "newest-session");
        }
    }

    @Nested
    @DisplayName("deactivateSessionByUserIdAndSessionId()")
    class DeactivateSessionByUserIdAndSessionId {

        @Test
        @DisplayName("활성 디바이스 → sessionId가 NULL로 변경되고 updated = 1 을 반환한다")
        void activeDevice_deactivatesSession_andReturnsOne() {
            DeviceEntity device = em.persistAndFlush(DeviceFixture.builder(userId)
                    .sessionId("target-session")
                    .build());

            int updated = deviceRepository.deactivateSessionByUserIdAndSessionId(
                    userId, "target-session");

            em.clear();   // 1차 캐시 비우고 DB 재조회 → @Modifying 결과를 정확히 검증
            DeviceEntity refreshed = em.find(DeviceEntity.class, device.getId());

            assertThat(updated).isEqualTo(1);
            assertThat(refreshed.getSessionId()).isNull();
            assertThat(refreshed.hasActiveSession()).isFalse();
        }

        @Test
        @DisplayName("이미 비활성 디바이스 → updated = 0 을 반환하고 예외 없이 종료한다 (멱등)")
        void inactiveDevice_returnsZero_withoutException() {
            em.persistAndFlush(DeviceFixture.inactiveDevice(userId));

            int updated = deviceRepository.deactivateSessionByUserIdAndSessionId(
                    userId, "non-existent-session");

            assertThat(updated).isZero();
        }

        @Test
        @DisplayName("다른 사용자의 세션을 비활성화할 수 없다 (userId 조건 격리)")
        void doesNotDeactivate_otherUsersSession() {
            DeviceEntity userDevice = em.persistAndFlush(DeviceFixture.builder(userId)
                    .fingerprint("fp-user-own")
                    .sessionId("user-own-session")
                    .build());

            Long other = persistOtherUser();
            DeviceEntity otherDevice = em.persistAndFlush(DeviceFixture.builder(other)
                    .sessionId("other-session")
                    .fingerprint("fp-other")
                    .build());

            int updated = deviceRepository.deactivateSessionByUserIdAndSessionId(
                    userId, "other-session");

            em.clear();
            DeviceEntity otherRefreshed = em.find(DeviceEntity.class, otherDevice.getId());
            DeviceEntity userOwnRefreshed = em.find(DeviceEntity.class, userDevice.getId());

            assertThat(updated).isZero();
            assertThat(otherRefreshed.getSessionId()).isEqualTo("other-session");
            assertThat(userOwnRefreshed.getSessionId()).isEqualTo("user-own-session");
        }

        @Test
        @DisplayName("동일 사용자의 다른 세션은 영향받지 않는다")
        void doesNotAffect_otherSessionsOfSameUser() {
            DeviceEntity target = em.persistAndFlush(DeviceFixture.builder(userId)
                    .fingerprint("fp-target")
                    .sessionId("target-session")
                    .build());
            DeviceEntity safe = em.persistAndFlush(DeviceFixture.builder(userId)
                    .fingerprint("fp-safe")
                    .sessionId("safe-session")
                    .build());

            deviceRepository.deactivateSessionByUserIdAndSessionId(userId, "target-session");

            em.clear();
            DeviceEntity targetRefreshed = em.find(DeviceEntity.class, target.getId());
            DeviceEntity safeRefreshed = em.find(DeviceEntity.class, safe.getId());

            assertThat(targetRefreshed.getSessionId()).isNull();
            assertThat(safeRefreshed.getSessionId()).isEqualTo("safe-session");
        }
    }

    private DeviceEntity persistDevice(String fingerprint, String sessionId, LocalDateTime loginAt) {
        return em.persistAndFlush(DeviceFixture.builder(userId)
                .fingerprint(fingerprint)
                .sessionId(sessionId)
                .loginAt(loginAt)
                .build());
    }

}