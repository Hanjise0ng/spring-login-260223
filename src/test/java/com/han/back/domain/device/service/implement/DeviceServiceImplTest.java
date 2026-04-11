package com.han.back.domain.device.service.implement;

import com.han.back.domain.device.dto.DeviceInfoDto;
import com.han.back.domain.device.dto.response.DeviceDetailResponseDto;
import com.han.back.domain.device.dto.response.DeviceReissueResponseDto;
import com.han.back.domain.device.dto.response.DeviceSignInResponseDto;
import com.han.back.domain.device.entity.DeviceEntity;
import com.han.back.domain.device.entity.DeviceType;
import com.han.back.domain.device.repository.DeviceRepository;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.fixture.DeviceFixture;
import com.han.back.fixture.UserFixture;
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
    @Mock private UserRepository userRepository;
    @Mock private TokenService tokenService;

    @InjectMocks private DeviceServiceImpl deviceService;

    private static final Long   USER_ID     = 1L;
    private static final String SESSION_ID  = "session-current-001";
    private static final String FINGERPRINT = "fp-web-default-001";
    private static final String UUID_REGEX = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private UserEntity user;
    private DeviceInfoDto webDeviceInfo;

    @BeforeEach
    void setUp() {
        user        = UserFixture.localUser();
        webDeviceInfo = DeviceFixture.webDeviceInfo();
    }

    @Nested
    @DisplayName("registerLoginDevice()")
    class RegisterLoginDevice {

        @BeforeEach
        void stubActiveDevicesUnderMax() {
            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of());
        }

        @Test
        @DisplayName("미등록 fingerprint → 신규 DeviceEntity 를 생성하고 save 한다")
        void newFingerprint_createsNewDevice() {
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.empty());
            given(userRepository.getReferenceById(USER_ID)).willReturn(user);

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(userRepository).should(times(1)).getReferenceById(USER_ID);
            then(deviceRepository).should(times(1)).save(any(DeviceEntity.class));
        }

        @Test
        @DisplayName("기존 fingerprint → 기존 Entity 를 재사용하고 신규 생성하지 않는다")
        void existingFingerprint_activatesExistingDevice() {
            // 조회 키와 Entity fingerprint를 일치시켜야 "같은 기기 재로그인" 시나리오가 성립
            DeviceEntity existing = DeviceFixture.builder(user)
                    .fingerprint(FINGERPRINT)   // "fp-web-default-001"
                    .sessionId(null)
                    .build();
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.of(existing));

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(userRepository).should(never()).getReferenceById(anyLong());
            then(deviceRepository).should(times(1)).save(existing);

            assertThat(existing.hasActiveSession()).isTrue();
            assertThat(existing.getSessionId()).matches(UUID_REGEX);
        }

        @Test
        @DisplayName("반환된 sessionId 는 UUID 포맷이고, fingerprint 는 입력값과 일치한다")
        void returns_sessionIdInUuidFormat_andFingerprint() {
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.empty());
            given(userRepository.getReferenceById(USER_ID)).willReturn(user);

            DeviceSignInResponseDto result =
                    deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            assertThat(result.getSessionId()).matches(UUID_REGEX);
            assertThat(result.getDeviceFingerprint()).isEqualTo(FINGERPRINT);
        }

        @Test
        @DisplayName("활성 세션 수가 MAX(2) 이하이면 세션 퇴출이 발생하지 않는다")
        void activeSessionsUnderMax_noEviction() {
            // 기존 활성 1 + 신규 1 = 2 → MAX(2) 이하이므로 퇴출 미발동
            DeviceEntity activeDevice = DeviceFixture.activeWebDevice(user);
            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of(activeDevice));
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.empty());
            given(userRepository.getReferenceById(USER_ID)).willReturn(user);

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        }

        @Test
        @DisplayName("활성 세션이 MAX + 1 (3개)이면 가장 오래된 세션을 퇴출한다")
        void activeSessionsExceedMax_evictsOldestSession() {
            // Repository 반환 순서: 오래된 순(ASC) → index 0(oldest)이 퇴출 대상
            DeviceEntity oldest = DeviceFixture.builder(user)
                    .sessionId("oldest-session")
                    .loginAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                    .build();
            DeviceEntity middle = DeviceFixture.builder(user)
                    .sessionId("middle-session")
                    .loginAt(LocalDateTime.of(2024, 6, 1, 0, 0))
                    .build();
            DeviceEntity newest = DeviceFixture.builder(user)
                    .sessionId("newest-session")
                    .loginAt(LocalDateTime.of(2024, 12, 1, 0, 0))
                    .build();

            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of(oldest, middle, newest));
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.empty());
            given(userRepository.getReferenceById(USER_ID)).willReturn(user);

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            // 가장 오래된 세션만 퇴출 나머지는 보존
            then(tokenService).should(times(1))
                    .invalidateSession(USER_ID, "oldest-session");
            then(tokenService).should(never())
                    .invalidateSession(USER_ID, "middle-session");
        }

        @Test
        @DisplayName("퇴출 대상은 excessCount 만큼으로 제한되고 나머지는 보존된다")
        void eviction_limitsToExcessCount_andPreservesRest() {
            DeviceEntity target = DeviceFixture.builder(user)
                    .sessionId("evict-this")
                    .loginAt(LocalDateTime.of(2024, 1, 1, 0, 0))
                    .build();
            DeviceEntity safe = DeviceFixture.builder(user)
                    .sessionId("keep-this")
                    .loginAt(LocalDateTime.of(2024, 6, 1, 0, 0))
                    .build();

            DeviceEntity newer = DeviceFixture.builder(user)
                    .sessionId("newer-session")
                    .loginAt(LocalDateTime.of(2024, 12, 1, 0, 0))
                    .build();

            given(deviceRepository.findActiveDevicesByUserIdOldestFirst(USER_ID))
                    .willReturn(List.of(target, safe, newer)); // 3개 → MAX(2) 초과
            given(deviceRepository.findByUserIdAndDeviceFingerprint(USER_ID, FINGERPRINT))
                    .willReturn(Optional.empty());
            given(userRepository.getReferenceById(USER_ID)).willReturn(user);

            deviceService.registerLoginDevice(USER_ID, webDeviceInfo);

            then(tokenService).should(times(1))
                    .invalidateSession(USER_ID, "evict-this");

            then(tokenService).should(never())
                    .invalidateSession(USER_ID, "keep-this");
            then(tokenService).should(never())
                    .invalidateSession(USER_ID, "newer-session");
        }
    }

    @Nested
    @DisplayName("rotateDeviceSession()")
    class RotateDeviceSession {

        @Test
        @DisplayName("존재하는 sessionId → 새 sessionId 를 UUID 포맷으로 반환하며 이전 값과 다르다")
        void existingSession_returnsNewSessionIdInUuidFormat() {
            DeviceEntity device = DeviceFixture.activeWebDevice(user);
            given(deviceRepository.findByUserIdAndSessionId(USER_ID, SESSION_ID))
                    .willReturn(Optional.of(device));

            DeviceReissueResponseDto result = deviceService.rotateDeviceSession(USER_ID, SESSION_ID);
            assertThat(result.getSessionId()).matches(UUID_REGEX);
            assertThat(result.getSessionId()).isNotEqualTo(SESSION_ID);
            assertThat(result.getDeviceType()).isEqualTo(DeviceType.WEB_DESKTOP);
            assertThat(device.getSessionId()).isEqualTo(result.getSessionId());
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
        @DisplayName("올바른 userId 와 sessionId 로 Repository 를 호출한다")
        void callsRepository_withCorrectParams() {
            given(deviceRepository.deactivateSessionByUserIdAndSessionId(USER_ID, SESSION_ID))
                    .willReturn(1);

            deviceService.deactivateSession(USER_ID, SESSION_ID);

            then(deviceRepository).should(times(1))
                    .deactivateSessionByUserIdAndSessionId(USER_ID, SESSION_ID);
        }

        @Test
        @DisplayName("updated = 0 (이미 비활성) → 예외 없이 정상 종료한다 (멱등 처리)")
        void noMatchingSession_doesNotThrow() {
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
        @DisplayName("DeviceEntity 목록을 DeviceDetailResponseDto 로 변환하여 반환한다")
        void returnsDeviceList_mappedFromEntities() {
            // Mock 대신 실제 Entity — DeviceDetailResponseDto.from() 내 도메인 로직까지 검증
            DeviceEntity webDevice = DeviceFixture.activeWebDevice(user);
            DeviceEntity appDevice = DeviceFixture.activeAppDevice(user);
            given(deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(USER_ID))
                    .willReturn(List.of(webDevice, appDevice));

            List<DeviceDetailResponseDto> result =
                    deviceService.getDevices(USER_ID, SESSION_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getDeviceType()).isEqualTo(DeviceType.WEB_DESKTOP.name());
            assertThat(result.get(1).getDeviceType()).isEqualTo(DeviceType.APP_IOS.name());
        }

        @Test
        @DisplayName("currentSessionId 와 일치하는 디바이스만 currentDevice = true 로 표시된다")
        void currentDevice_isMarkedCorrectly() {
            DeviceEntity currentDevice = DeviceFixture.builder(user)
                    .sessionId(SESSION_ID)
                    .fingerprint("fp-current")
                    .build();
            DeviceEntity otherDevice = DeviceFixture.builder(user)
                    .sessionId("other-session")
                    .fingerprint("fp-other")
                    .build();
            DeviceEntity inactiveDevice = DeviceFixture.builder(user)
                    .sessionId(null)
                    .fingerprint("fp-inactive")
                    .build();

            given(deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(USER_ID))
                    .willReturn(List.of(currentDevice, otherDevice, inactiveDevice));

            List<DeviceDetailResponseDto> result =
                    deviceService.getDevices(USER_ID, SESSION_ID);

            assertThat(result.get(0).isCurrentDevice()).isTrue();
            assertThat(result.get(1).isCurrentDevice()).isFalse();
            assertThat(result.get(2).isCurrentDevice()).isFalse();

            assertThat(result.get(0).isActiveSession()).isTrue();
            assertThat(result.get(1).isActiveSession()).isTrue();
            assertThat(result.get(2).isActiveSession()).isFalse();
        }

        @Test
        @DisplayName("디바이스가 없는 사용자는 빈 리스트를 반환한다")
        void noDevices_returnsEmptyList() {
            given(deviceRepository.findAllByUserIdOrderByLastLoginAtDesc(USER_ID))
                    .willReturn(List.of());

            List<DeviceDetailResponseDto> result =
                    deviceService.getDevices(USER_ID, SESSION_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("forceLogoutDevice()")
    class ForceLogoutDevice {

        private static final String DEVICE_PUBLIC_ID = "device-public-uuid-001";

        @Test
        @DisplayName("다른 활성 디바이스 → invalidateSession + deactivateSession 이 호출된다")
        void activeOtherDevice_invalidatesAndDeactivates() {
            DeviceEntity otherDevice = DeviceFixture.builder(user)
                    .sessionId("other-session")
                    .fingerprint("fp-other")
                    .build();

            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PUBLIC_ID, USER_ID))
                    .willReturn(Optional.of(otherDevice));

            deviceService.forceLogoutDevice(USER_ID, DEVICE_PUBLIC_ID, SESSION_ID);

            then(tokenService).should(times(1))
                    .invalidateSession(USER_ID, "other-session");
            assertThat(otherDevice.hasActiveSession()).isFalse();
        }

        @Test
        @DisplayName("비활성 디바이스 → invalidateSession 을 호출하지 않는다 (멱등 처리)")
        void inactiveDevice_doesNothing() {
            DeviceEntity inactiveDevice = DeviceFixture.inactiveDevice(user);
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PUBLIC_ID, USER_ID))
                    .willReturn(Optional.of(inactiveDevice));

            assertThatCode(() ->
                    deviceService.forceLogoutDevice(USER_ID, DEVICE_PUBLIC_ID, SESSION_ID))
                    .doesNotThrowAnyException();

            then(tokenService).should(never()).invalidateSession(anyLong(), anyString());
        }

        @Test
        @DisplayName("자기 자신의 디바이스를 강제 로그아웃 시도 → SELF_DEVICE_FORCE_LOGOUT 예외")
        void currentDevice_throwsSelfDeviceForceLogout() {
            DeviceEntity selfDevice = DeviceFixture.builder(user)
                    .sessionId(SESSION_ID)
                    .fingerprint("fp-self")
                    .build();
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PUBLIC_ID, USER_ID))
                    .willReturn(Optional.of(selfDevice));

            assertThatThrownBy(() ->
                    deviceService.forceLogoutDevice(USER_ID, DEVICE_PUBLIC_ID, SESSION_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.SELF_DEVICE_FORCE_LOGOUT);
        }

        @Test
        @DisplayName("존재하지 않는 publicId → NOT_FOUND_DEVICE 예외를 던진다")
        void notFoundDevice_throwsNotFoundDevice() {
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PUBLIC_ID, USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    deviceService.forceLogoutDevice(USER_ID, DEVICE_PUBLIC_ID, SESSION_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.NOT_FOUND_DEVICE);
        }
    }

    @Nested
    @DisplayName("deleteDevice()")
    class DeleteDevice {

        private static final String DEVICE_PUBLIC_ID = "device-public-uuid-002";

        @Test
        @DisplayName("비활성 디바이스 → delete 정확히 1번 호출된다")
        void inactiveDevice_deletesSuccessfully() {
            DeviceEntity inactiveDevice = DeviceFixture.inactiveDevice(user);
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PUBLIC_ID, USER_ID))
                    .willReturn(Optional.of(inactiveDevice));

            deviceService.deleteDevice(USER_ID, DEVICE_PUBLIC_ID);

            then(deviceRepository).should(times(1)).delete(inactiveDevice);
        }

        @Test
        @DisplayName("활성 세션이 있는 디바이스 삭제 시도 → ACTIVE_DEVICE_CANNOT_DELETE 예외")
        void activeDevice_throwsActiveDeviceCannotDelete() {
            DeviceEntity activeDevice = DeviceFixture.activeWebDevice(user);
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PUBLIC_ID, USER_ID))
                    .willReturn(Optional.of(activeDevice));

            assertThatThrownBy(() -> deviceService.deleteDevice(USER_ID, DEVICE_PUBLIC_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.ACTIVE_DEVICE_CANNOT_DELETE);

            then(deviceRepository).should(never()).delete(any());
        }

        @Test
        @DisplayName("존재하지 않는 publicId → NOT_FOUND_DEVICE 예외를 던진다")
        void notFoundDevice_throwsNotFoundDevice() {
            given(deviceRepository.findByPublicIdAndUserId(DEVICE_PUBLIC_ID, USER_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.deleteDevice(USER_ID, DEVICE_PUBLIC_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.NOT_FOUND_DEVICE);
        }
    }

}