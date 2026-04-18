package com.han.back.domain.device.service.implement;

import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.domain.device.dto.DeviceRegistration;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.entity.DeviceType;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.fixture.DeviceFixture;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.security.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

    @InjectMocks private DeviceServiceImpl deviceService;

    private static final Long   USER_ID      = 1L;
    private static final String SESSION_ID   = "session-current-001";
    private static final String FINGERPRINT  = DeviceFixture.DEFAULT_WEB_FINGERPRINT;
    private static final String DEVICE_PID   = "device-public-uuid-001";

    private DeviceInfo webDeviceInfo;

    @BeforeEach
    void setUp() {
        webDeviceInfo = DeviceFixture.webDeviceInfo();
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
        @DisplayName("반환값은 Entity의 sessionId와 입력 fingerprint를 담는다")
        void returns_sessionIdMatchingEntity_andInputFingerprint() {
            givenNoExistingDevice();
            givenActiveDevicesUnderMax();

            DeviceRegistration result = deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            assertThat(result.getSessionId()).isNotBlank();
            assertThat(result.getDeviceFingerprint()).isEqualTo(FINGERPRINT);
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
        @DisplayName("활성 세션이 MAX 초과 → 가장 오래된 세션만 퇴출하고 나머지는 보존한다")
        void activeSessionsExceedMax_evictsOldestOnly() {
            // Repository 반환 순서: 오래된 순(ASC) → index 0 = 퇴출 대상
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
            assertThat(oldest.hasActiveSession()).isFalse();
            assertThat(middle.hasActiveSession()).isTrue();
            assertThat(newest.hasActiveSession()).isTrue();
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
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);
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
                    .isEqualTo(BaseResponseStatus.SELF_DEVICE_FORCE_LOGOUT);

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
                    .isEqualTo(BaseResponseStatus.NOT_FOUND_DEVICE);
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
                    .isEqualTo(BaseResponseStatus.ACTIVE_DEVICE_CANNOT_DELETE);

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
                    .isEqualTo(BaseResponseStatus.NOT_FOUND_DEVICE);
        }
    }

}