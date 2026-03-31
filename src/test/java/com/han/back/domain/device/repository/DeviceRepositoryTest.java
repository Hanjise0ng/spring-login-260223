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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DeviceRepository")
class DeviceRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired DeviceRepository deviceRepository;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(UserFixture.localUser());
    }

    @Nested
    @DisplayName("findAllByUserIdOrderByLastLoginAtDesc()")
    class FindAllByUserIdOrderByLastLoginAtDesc {

        @Test
        @DisplayName("최근 로그인 순(DESC)으로 반환한다")
        void returnsDevices_orderedByLastLoginAtDesc() {
            DeviceEntity oldest = DeviceFixture.builder(user)
                    .fingerprint("fp-oldest")
                    .loginAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .sessionId("session-oldest")
                    .build();
            DeviceEntity middle = DeviceFixture.builder(user)
                    .fingerprint("fp-middle")
                    .loginAt(LocalDateTime.of(2025, 6, 1, 0, 0))
                    .sessionId("session-middle")
                    .build();
            DeviceEntity newest = DeviceFixture.builder(user)
                    .fingerprint("fp-newest")
                    .loginAt(LocalDateTime.of(2025, 12, 1, 0, 0))
                    .sessionId("session-newest")
                    .build();

            em.persistAndFlush(oldest);
            em.persistAndFlush(middle);
            em.persistAndFlush(newest);

            List<DeviceEntity> result =
                    deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId());

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getDeviceFingerprint()).isEqualTo("fp-newest");
            assertThat(result.get(1).getDeviceFingerprint()).isEqualTo("fp-middle");
            assertThat(result.get(2).getDeviceFingerprint()).isEqualTo("fp-oldest");
        }

        @Test
        @DisplayName("다른 사용자의 디바이스는 결과에 포함되지 않는다")
        void excludesOtherUsersDevices() {
            UserEntity otherUser = em.persistAndFlush(UserFixture.adminUser());

            em.persistAndFlush(DeviceFixture.activeWebDevice(user));
            em.persistAndFlush(DeviceFixture.builder(otherUser)
                    .fingerprint("fp-other-user")
                    .build());

            List<DeviceEntity> result =
                    deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDeviceFingerprint()).isEqualTo("fp-web-default-001");
        }

        @Test
        @DisplayName("디바이스가 없으면 빈 리스트를 반환한다")
        void noDevices_returnsEmptyList() {
            List<DeviceEntity> result =
                    deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(user.getId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findActiveDevicesByUserIdOldestFirst()")
    class FindActiveDevicesByUserIdOldestFirst {

        @Test
        @DisplayName("sessionId IS NOT NULL 인 디바이스만 오래된 순(ASC)으로 반환한다")
        void returnsOnlyActiveDevices_orderedByLastLoginAtAsc() {
            DeviceEntity oldest = DeviceFixture.builder(user)
                    .fingerprint("fp-oldest")
                    .loginAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .sessionId("session-oldest")
                    .build();
            DeviceEntity newest = DeviceFixture.builder(user)
                    .fingerprint("fp-newest")
                    .loginAt(LocalDateTime.of(2025, 12, 1, 0, 0))
                    .sessionId("session-newest")
                    .build();

            DeviceEntity inactive = DeviceFixture.builder(user)
                    .fingerprint("fp-inactive")
                    .loginAt(LocalDateTime.of(2025, 6, 1, 0, 0))
                    .sessionId(null)
                    .build();

            em.persistAndFlush(oldest);
            em.persistAndFlush(newest);
            em.persistAndFlush(inactive);

            List<DeviceEntity> result =
                    deviceRepository.findActiveDevicesByUserIdOldestFirst(user.getId());

            // 활성 2개만 반환 + 오래된 순(ASC)
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDeviceFingerprint()).isEqualTo("fp-oldest");
            assertThat(result.get(1).getDeviceFingerprint()).isEqualTo("fp-newest");
        }

        @Test
        @DisplayName("비활성 디바이스만 있으면 빈 리스트를 반환한다")
        void allInactive_returnsEmptyList() {
            em.persistAndFlush(DeviceFixture.inactiveDevice(user));

            List<DeviceEntity> result =
                    deviceRepository.findActiveDevicesByUserIdOldestFirst(user.getId());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("최대 세션 정책에서 index 0이 가장 오래된 퇴출 대상임을 보장한다")
        void firstElement_isOldestSession_forEvictionPolicy() {
            em.persistAndFlush(DeviceFixture.builder(user)
                    .fingerprint("fp-first")
                    .loginAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                    .sessionId("oldest-session")
                    .build());
            em.persistAndFlush(DeviceFixture.builder(user)
                    .fingerprint("fp-second")
                    .loginAt(LocalDateTime.of(2025, 6, 1, 0, 0))
                    .sessionId("middle-session")
                    .build());
            em.persistAndFlush(DeviceFixture.builder(user)
                    .fingerprint("fp-third")
                    .loginAt(LocalDateTime.of(2025, 12, 1, 0, 0))
                    .sessionId("newest-session")
                    .build());

            List<DeviceEntity> result =
                    deviceRepository.findActiveDevicesByUserIdOldestFirst(user.getId());

            assertThat(result.get(0).getSessionId()).isEqualTo("oldest-session");
            assertThat(result.get(2).getSessionId()).isEqualTo("newest-session");
        }
    }

    @Nested
    @DisplayName("deactivateSessionByUserIdAndSessionId()")
    class DeactivateSessionByUserIdAndSessionId {

        @Test
        @DisplayName("활성 디바이스 → sessionId가 NULL로 변경되고 updated = 1 을 반환한다")
        void activeDevice_deactivatesSession_andReturnsOne() {
            DeviceEntity device = em.persistAndFlush(
                    DeviceFixture.builder(user)
                            .sessionId("target-session")
                            .build()
            );

            int updated = deviceRepository.deactivateSessionByUserIdAndSessionId(
                    user.getId(), "target-session"
            );

            DeviceEntity refreshed = em.find(DeviceEntity.class, device.getId());

            assertThat(updated).isEqualTo(1);
            assertThat(refreshed.getSessionId()).isNull();
            assertThat(refreshed.hasActiveSession()).isFalse();
        }

        @Test
        @DisplayName("이미 비활성 디바이스 → updated = 0 을 반환하고 예외 없이 종료한다 (멱등)")
        void inactiveDevice_returnsZero_withoutException() {
            em.persistAndFlush(DeviceFixture.inactiveDevice(user));

            int updated = deviceRepository.deactivateSessionByUserIdAndSessionId(
                    user.getId(), "non-existent-session"
            );

            assertThat(updated).isZero();
        }

        @Test
        @DisplayName("다른 사용자의 세션을 비활성화할 수 없다 (userId 조건 격리)")
        void doesNotDeactivate_otherUsersSession() {
            DeviceEntity userDevice = em.persistAndFlush(
                    DeviceFixture.builder(user)
                            .fingerprint("fp-user-own")
                            .sessionId("user-own-session")
                            .build()
            );

            UserEntity otherUser = em.persistAndFlush(UserFixture.adminUser());
            DeviceEntity otherDevice = em.persistAndFlush(
                    DeviceFixture.builder(otherUser)
                            .sessionId("other-session")
                            .fingerprint("fp-other")
                            .build()
            );

            // user.getId()로 otherUser의 sessionId 비활성화 시도 (IDOR 공격 시나리오)
            int updated = deviceRepository.deactivateSessionByUserIdAndSessionId(
                    user.getId(), "other-session"
            );

            DeviceEntity otherRefreshed = em.find(DeviceEntity.class, otherDevice.getId());
            DeviceEntity userOwnRefreshed = em.find(DeviceEntity.class, userDevice.getId());

            assertThat(updated).isZero();
            assertThat(otherRefreshed.getSessionId()).isEqualTo("other-session");
            assertThat(userOwnRefreshed.getSessionId()).isEqualTo("user-own-session");
        }

        @Test
        @DisplayName("동일 사용자의 다른 세션은 영향받지 않는다")
        void doesNotAffect_otherSessionsOfSameUser() {
            DeviceEntity target = em.persistAndFlush(
                    DeviceFixture.builder(user)
                            .fingerprint("fp-target")
                            .sessionId("target-session")
                            .build()
            );
            DeviceEntity safe = em.persistAndFlush(
                    DeviceFixture.builder(user)
                            .fingerprint("fp-safe")
                            .sessionId("safe-session")
                            .build()
            );

            deviceRepository.deactivateSessionByUserIdAndSessionId(
                    user.getId(), "target-session"
            );

            DeviceEntity targetRefreshed = em.find(DeviceEntity.class, target.getId());
            DeviceEntity safeRefreshed = em.find(DeviceEntity.class, safe.getId());

            assertThat(targetRefreshed.getSessionId()).isNull();
            assertThat(safeRefreshed.getSessionId()).isEqualTo("safe-session");
        }
    }

}
