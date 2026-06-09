package com.han.back.domain.device.service.implement;

import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.entity.DeviceType;
import com.han.back.domain.device.exception.DeviceResponseStatus;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.fixture.DeviceFixture;
import com.han.back.global.device.DeviceProperties;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeviceServiceImpl")
class DeviceServiceImplTest {

    @Mock private DeviceRepository deviceRepository;
    @Mock private TokenService tokenService;

    private DeviceServiceImpl deviceService;
    private DeviceInfo webDeviceInfo;

    private static final Long USER_ID = 1L;
    private static final String SESSION_ID = "session-current-001";
    private static final String FINGERPRINT = DeviceFixture.DEFAULT_WEB_FINGERPRINT;
    private static final String DEVICE_PID = "device-public-uuid-001";
    private static final int MAX_SESSIONS = 2;
    private static final int MAX_TRUSTED = 2;

    @BeforeEach
    void setUp() {
        DeviceProperties properties = new DeviceProperties(MAX_SESSIONS, MAX_TRUSTED);
        deviceService = new DeviceServiceImpl(deviceRepository, tokenService, properties);
        webDeviceInfo = DeviceFixture.webDeviceInfo();
    }

    private void givenNoExistingDevice() {
        given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                .willReturn(Optional.empty());
    }

    private void givenActiveDevicesUnderMax() {
        given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                .willReturn(List.of());
    }

    private DeviceEntity activeDeviceWith(String sessionId, LocalDateTime loginAt) {
        return DeviceFixture.builder(USER_ID)
                .sessionId(sessionId)
                .fingerprint("fp-" + sessionId)
                .loginAt(loginAt)
                .build();
    }

    private DeviceEntity trustedDeviceWith(String sessionId, LocalDateTime loginAt) {
        return DeviceFixture.builder(USER_ID)
                .sessionId(sessionId)
                .fingerprint("fp-trusted-" + sessionId)
                .loginAt(loginAt)
                .trusted(true)
                .build();
    }


    @Nested
    @DisplayName("registerLoginDevice()")
    class RegisterLoginDevice {

        @Test
        @DisplayName("미등록 fingerprint → 신규 DeviceEntity를 생성하고 save 한다")
        void newFingerprint_createsNewDevice() {
            givenNoExistingDevice();
            givenActiveDevicesUnderMax();

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(deviceRepository).should().save(any(DeviceEntity.class));
        }

        @Test
        @DisplayName("기존 fingerprint → 기존 Entity를 재사용하고 새 sessionId로 활성화한다")
        void existingFingerprint_activatesExistingDevice() {
            DeviceEntity existing = DeviceFixture.builder(USER_ID)
                    .fingerprint(FINGERPRINT)
                    .sessionId(null)
                    .build();
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.of(existing));
            givenActiveDevicesUnderMax();

            DeviceRegistration result = deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(deviceRepository).should().save(existing);
            // 반환 sessionId 와 Entity 의 sessionId 가 동일 → 활성화 검증
            assertThat(existing.getSessionId()).isEqualTo(result.getSessionId());
            assertThat(existing.hasActiveSession()).isTrue();
        }

        @Test
        @DisplayName("반환값은 Entity의 sessionId를 담는다")
        void returns_sessionIdMatchingEntity_andInputFingerprint() {
            givenNoExistingDevice();
            givenActiveDevicesUnderMax();

            DeviceRegistration result = deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            assertThat(result.getSessionId()).isNotBlank();
        }

        @Test
        @DisplayName("활성 세션 수가 MAX 이하이면 퇴출이 발생하지 않는다")
        void activeSessionsUnderMax_noEviction() {
            givenNoExistingDevice();
            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of(DeviceFixture.activeWebDevice(USER_ID)));

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        }

        @Test
        @DisplayName("활성 세션이 MAX 초과 → 가장 오래된 일반 기기 세션만 퇴출한다")
        void activeSessionsExceedMax_evictsOldestUntrustedOnly() {
            DeviceEntity oldest = activeDeviceWith("oldest-session",
                    LocalDateTime.of(2024, 1, 1, 0, 0));
            DeviceEntity middle = activeDeviceWith("middle-session",
                    LocalDateTime.of(2024, 6, 1, 0, 0));
            DeviceEntity newest = activeDeviceWith("newest-session",
                    LocalDateTime.of(2024, 12, 1, 0, 0));

            givenNoExistingDevice();
            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of(oldest, middle, newest));

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(tokenService).should().invalidateSession(USER_ID, "oldest-session");
            then(tokenService).should(never()).invalidateSession(USER_ID, "middle-session");
            then(tokenService).should(never()).invalidateSession(USER_ID, "newest-session");
        }

        private void givenNoExistingDevice() {
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.empty());
        }

        private void givenActiveDevicesUnderMax() {
            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of());
        }

        private DeviceEntity activeDeviceWith(String sessionId, LocalDateTime loginAt) {
            return DeviceFixture.builder(USER_ID)
                    .sessionId(sessionId)
                    .fingerprint("fp-" + sessionId)
                    .loginAt(loginAt)
                    .build();
        }
    }

    @Nested
    @DisplayName("rotateDeviceSession()")
    class RotateDeviceSession {

        @Test
        @DisplayName("존재하는 sessionId → 새 sessionId가 Entity에 반영되고 반환값과 일치한다")
        void existingSession_rotatesSessionIdOnEntity() {
            DeviceEntity device = DeviceFixture.builder(USER_ID)
                    .sessionId(SESSION_ID)
                    .build();
            given(deviceRepository.findByUserIdAndSessionId(USER_ID, SESSION_ID))
                    .willReturn(Optional.of(device));

            String newSessionId = deviceService.rotateDeviceSession(USER_ID, SESSION_ID);

            assertThat(newSessionId).isNotBlank().isNotEqualTo(SESSION_ID);
            assertThat(device.getSessionId()).isEqualTo(newSessionId);
        }

        @Test
        @DisplayName("존재하지 않는 sessionId → AUTHENTICATION_FAIL 예외를 던진다")
        void nonExistentSession_throwsAuthenticationFail() {
            given(deviceRepository.findByUserIdAndSessionId(USER_ID, SESSION_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.rotateDeviceSession(USER_ID, SESSION_ID))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL);
        }
    }

    @Nested
    @DisplayName("deactivateSession()")
    class DeactivateSession {

        @Test
        @DisplayName("updated = 1 → Repository가 호출되고 정상 종료")
        void updatedOne_callsRepository() {
            given(deviceRepository.deactivateSessionByUserIdAndSessionId(USER_ID, SESSION_ID))
                    .willReturn(1);

            deviceService.deactivateSession(USER_ID, SESSION_ID);

            then(deviceRepository).should()
                    .deactivateSessionByUserIdAndSessionId(USER_ID, SESSION_ID);
        }

        @Test
        @DisplayName("updated = 0 (이미 비활성) → 예외 없이 정상 종료 (멱등)")
        void updatedZero_doesNotThrow() {
            given(deviceRepository.deactivateSessionByUserIdAndSessionId(USER_ID, SESSION_ID))
                    .willReturn(0);

            assertThatCode(() -> deviceService.deactivateSession(USER_ID, SESSION_ID))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getDevices()")
    class GetDevices {

        @Test
        @DisplayName("Entity 목록을 DeviceDetailResponseDto로 변환하여 반환한다")
        void returnsDeviceList_mappedFromEntities() {
            DeviceEntity webDevice = DeviceFixture.activeWebDevice(USER_ID);
            DeviceEntity appDevice = DeviceFixture.activeAppDevice(USER_ID);
            given(deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(USER_ID))
                    .willReturn(List.of(webDevice, appDevice));

            List<DeviceDetailResponseDto> result = deviceService.getDevices(USER_ID, SESSION_ID);

            assertThat(result)
                    .extracting(DeviceDetailResponseDto::getDeviceType)
                    .containsExactly(DeviceType.WEB_DESKTOP.name(), DeviceType.APP_IOS.name());
        }

        @Test
        @DisplayName("currentSessionId와 일치하는 디바이스만 currentDevice = true로 표시된다")
        void currentDevice_isMarkedCorrectly() {
            DeviceEntity current = DeviceFixture.builder(USER_ID)
                    .sessionId(SESSION_ID).fingerprint("fp-current").build();
            DeviceEntity other = DeviceFixture.builder(USER_ID)
                    .sessionId("other-session").fingerprint("fp-other").build();
            DeviceEntity inactive = DeviceFixture.builder(USER_ID)
                    .sessionId(null).fingerprint("fp-inactive").build();

            given(deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(USER_ID))
                    .willReturn(List.of(current, other, inactive));

            List<DeviceDetailResponseDto> result = deviceService.getDevices(USER_ID, SESSION_ID);

            assertThat(result)
                    .extracting(
                            DeviceDetailResponseDto::isCurrentDevice,
                            DeviceDetailResponseDto::isActiveSession)
                    .containsExactly(
                            tuple(true,  true),
                            tuple(false, true),
                            tuple(false, false));
        }

        @Test
        @DisplayName("디바이스가 없으면 빈 리스트를 반환한다")
        void noDevices_returnsEmptyList() {
            given(deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(USER_ID))
                    .willReturn(List.of());

            assertThat(deviceService.getDevices(USER_ID, SESSION_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("forceLogoutDevice()")
    class ForceLogoutDevice {

        @Test
        @DisplayName("다른 활성 디바이스 → invalidateSession + deactivateSession 호출")
        void activeOtherDevice_invalidatesAndDeactivates() {
            DeviceEntity target = DeviceFixture.builder(USER_ID)
                    .sessionId("other-session").fingerprint("fp-other").build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(target));

            deviceService.forceLogoutDevice(USER_ID, DEVICE_PID, SESSION_ID);

            then(tokenService).should().invalidateSession(USER_ID, "other-session");
            assertThat(target.hasActiveSession()).isFalse();
        }

        @Test
        @DisplayName("비활성 디바이스 → 예외 없이 invalidateSession 호출하지 않는다 (멱등)")
        void inactiveDevice_doesNothing() {
            DeviceEntity inactive = DeviceFixture.inactiveDevice(USER_ID);
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(inactive));

            assertThatCode(() ->
                    deviceService.forceLogoutDevice(USER_ID, DEVICE_PID, SESSION_ID))
                    .doesNotThrowAnyException();

            then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        }

        @Test
        @DisplayName("현재 세션의 디바이스 → SELF_DEVICE_FORCE_LOGOUT")
        void currentDevice_throwsSelfForceLogout() {
            DeviceEntity self = DeviceFixture.builder(USER_ID)
                    .sessionId(SESSION_ID).fingerprint("fp-self").build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(self));

            assertThatThrownBy(() ->
                    deviceService.forceLogoutDevice(USER_ID, DEVICE_PID, SESSION_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(DeviceResponseStatus.DEVICE_SELF_LOGOUT_FORBIDDEN);

            then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        }

        @Test
        @DisplayName("존재하지 않는 publicId → NOT_FOUND_DEVICE")
        void notFound_throwsNotFoundDevice() {
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    deviceService.forceLogoutDevice(USER_ID, DEVICE_PID, SESSION_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(DeviceResponseStatus.DEVICE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteDevice()")
    class DeleteDevice {

        @Test
        @DisplayName("비활성 디바이스 → delete 호출")
        void inactiveDevice_deletesSuccessfully() {
            DeviceEntity inactive = DeviceFixture.inactiveDevice(USER_ID);
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(inactive));

            deviceService.deleteDevice(USER_ID, DEVICE_PID);

            then(deviceRepository).should().delete(inactive);
        }

        @Test
        @DisplayName("활성 디바이스 삭제 시도 → ACTIVE_DEVICE_CANNOT_DELETE")
        void activeDevice_throwsActiveDeviceCannotDelete() {
            DeviceEntity active = DeviceFixture.activeWebDevice(USER_ID);
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(active));

            assertThatThrownBy(() -> deviceService.deleteDevice(USER_ID, DEVICE_PID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(DeviceResponseStatus.DEVICE_ACTIVE_DELETE_FORBIDDEN);

            then(deviceRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 publicId → NOT_FOUND_DEVICE")
        void notFound_throwsNotFoundDevice() {
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.deleteDevice(USER_ID, DEVICE_PID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(DeviceResponseStatus.DEVICE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("안심 기기 퇴출 정책")
    class TrustedDeviceEviction {

        @Test
        @DisplayName("안심 기기는 MAX 초과 시에도 퇴출되지 않는다")
        void trustedDevice_notEvictedOnMaxExceed() {
            DeviceEntity trustedOldest = trustedDeviceWith("trusted-session",
                    LocalDateTime.of(2024, 1, 1, 0, 0));
            DeviceEntity normalMiddle = activeDeviceWith("normal-session",
                    LocalDateTime.of(2024, 6, 1, 0, 0));
            DeviceEntity normalNewest = activeDeviceWith("newest-session",
                    LocalDateTime.of(2024, 12, 1, 0, 0));

            givenNoExistingDevice();
            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of(trustedOldest, normalMiddle, normalNewest));

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            // 안심 기기는 퇴출 안 됨, 일반 기기(normalMiddle)가 퇴출됨
            then(tokenService).should(never()).invalidateSession(USER_ID, "trusted-session");
            then(tokenService).should().invalidateSession(USER_ID, "normal-session");
        }

        @Test
        @DisplayName("모두 안심 기기일 때 새 로그인 → 퇴출 없이 세션 수 초과 허용")
        void allTrusted_noEvictionAllowed() {
            DeviceEntity trusted1 = trustedDeviceWith("trusted-1",
                    LocalDateTime.of(2024, 1, 1, 0, 0));
            DeviceEntity trusted2 = trustedDeviceWith("trusted-2",
                    LocalDateTime.of(2024, 6, 1, 0, 0));
            DeviceEntity trusted3 = trustedDeviceWith("trusted-3",
                    LocalDateTime.of(2024, 12, 1, 0, 0));

            givenNoExistingDevice();
            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of(trusted1, trusted2, trusted3));

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        }
    }

    // --- trustDevice 테스트 추가 ---

    @Nested
    @DisplayName("trustDevice()")
    class TrustDevice {

        @Test
        @DisplayName("일반 기기 → 안심 기기로 등록된다")
        void normalDevice_markedAsTrusted() {
            DeviceEntity device = DeviceFixture.builder(USER_ID)
                    .fingerprint("fp-trust").sessionId("s1").build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(device));
            given(deviceRepository.countTrustedDevicesByUserId(USER_ID))
                    .willReturn(0);

            deviceService.trustDevice(USER_ID, DEVICE_PID);

            assertThat(device.isTrusted()).isTrue();
        }

        @Test
        @DisplayName("이미 안심 기기 → 멱등 처리, 카운트 조회 없이 그냥 반환")
        void alreadyTrusted_idempotent() {
            DeviceEntity device = DeviceFixture.builder(USER_ID)
                    .fingerprint("fp-trust").sessionId("s1").trusted(true).build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(device));

            deviceService.trustDevice(USER_ID, DEVICE_PID);

            then(deviceRepository).should(never()).countTrustedDevicesByUserId(anyLong());
            assertThat(device.isTrusted()).isTrue();
        }

        @Test
        @DisplayName("안심 기기 한도 초과 → TRUSTED_DEVICE_LIMIT_EXCEEDED")
        void trustedLimitExceeded_throwsException() {
            DeviceEntity device = DeviceFixture.builder(USER_ID)
                    .fingerprint("fp-trust").sessionId("s1").build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(device));
            given(deviceRepository.countTrustedDevicesByUserId(USER_ID))
                    .willReturn(MAX_TRUSTED);  // 이미 한도 도달

            assertThatThrownBy(() -> deviceService.trustDevice(USER_ID, DEVICE_PID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(DeviceResponseStatus.DEVICE_TRUSTED_LIMIT_EXCEEDED);
        }

        @Test
        @DisplayName("존재하지 않는 기기 → NOT_FOUND_DEVICE")
        void deviceNotFound_throwsException() {
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.trustDevice(USER_ID, DEVICE_PID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(DeviceResponseStatus.DEVICE_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("untrustDevice()")
    class UntrustDevice {

        @Test
        @DisplayName("안심 기기 → 일반 기기로 해제된다")
        void trustedDevice_unmarkedAsTrusted() {
            DeviceEntity device = DeviceFixture.builder(USER_ID)
                    .fingerprint("fp-trust").sessionId("s1").trusted(true).build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(device));

            deviceService.untrustDevice(USER_ID, DEVICE_PID);

            assertThat(device.isTrusted()).isFalse();
        }

        @Test
        @DisplayName("이미 일반 기기 → 멱등 처리")
        void alreadyUntrusted_idempotent() {
            DeviceEntity device = DeviceFixture.builder(USER_ID)
                    .fingerprint("fp-trust").sessionId("s1").build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PID, USER_ID))
                    .willReturn(Optional.of(device));

            assertThatCode(() -> deviceService.untrustDevice(USER_ID, DEVICE_PID))
                    .doesNotThrowAnyException();

            assertThat(device.isTrusted()).isFalse();
        }
    }

}