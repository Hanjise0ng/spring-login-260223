package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.mapper.UserMapper;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.fixture.TokenFixture;
import com.han.back.fixture.UserFixture;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.LoginIdTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private DeviceService deviceService;
    @Mock private VerificationService verificationService;
    @Mock private LoginIdTokenUtil loginIdTokenUtil;

    @InjectMocks private AuthServiceImpl authService;

    private static final Long   USER_PK        = 1L;
    private static final Role   ROLE           = Role.USER;
    private static final String LOGIN_ID       = UserFixture.DEFAULT_LOGIN_ID;
    private static final String EMAIL          = "test@test.com";
    private static final String LOGIN_ID_TOKEN = "valid.loginid.token";
    private static final String SESSION_ID     = "session-abc-123";
    private static final String NEW_SESSION_ID = "new-session-xyz-456";
    private static final String ENCODED_PW     = "$2a$10$encodedPasswordForTest";

    @Nested
    @DisplayName("checkLoginId()")
    class CheckLoginId {

        @Test
        @DisplayName("이미 존재하는 loginId → DUPLICATE_ID 예외를 던지고 토큰을 발급하지 않는다")
        void existingLoginId_throwsDuplicateId() {
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(true);

            assertThatThrownBy(() -> authService.checkLoginId(LOGIN_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.DUPLICATE_ID);

            // 중복이면 토큰 발급까지 도달하지 않아야 함
            then(loginIdTokenUtil).should(never()).issue(anyString());
        }

        @Test
        @DisplayName("사용 가능한 loginId → loginIdToken이 담긴 LoginIdCheckResponseDto를 반환한다")
        void availableLoginId_returnsLoginIdCheckResponseDto() {
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(false);
            given(loginIdTokenUtil.issue(LOGIN_ID)).willReturn(LOGIN_ID_TOKEN);

            LoginIdCheckResponseDto result = authService.checkLoginId(LOGIN_ID);

            assertThat(result.getLoginIdToken()).isEqualTo(LOGIN_ID_TOKEN);
            then(loginIdTokenUtil).should(times(1)).issue(LOGIN_ID);
        }
    }

    @Nested
    @DisplayName("signUp()")
    class SignUp {

        @Test
        @DisplayName("loginIdToken 검증 실패 → LOGIN_ID_CHECK_REQUIRED를 던지고 이후 로직을 실행하지 않는다")
        void invalidLoginIdToken_throwsLoginIdCheckRequired() {
            // validate()는 loginId + loginIdToken 두 인자를 받음
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            willThrow(new CustomException(BaseResponseStatus.LOGIN_ID_CHECK_REQUIRED))
                    .given(loginIdTokenUtil).validate(LOGIN_ID, LOGIN_ID_TOKEN);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.LOGIN_ID_CHECK_REQUIRED);

            // 토큰 검증 실패 시 이후 모든 로직 미호출
            then(verificationService).should(never()).validateConfirmed(anyString(), any());
            then(userRepository).should(never()).existsByLoginId(anyString());
            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("이메일 인증 미완료 → VERIFICATION_NOT_COMPLETED를 던지고 DB 접근을 하지 않는다")
        void emailNotVerified_throwsVerificationNotCompleted() {
            // loginIdToken 검증 통과 (void 메서드 — @Mock 기본 동작: 아무것도 안 함)
            // validateConfirmed()는 email + type 두 인자를 받음
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            willThrow(new CustomException(BaseResponseStatus.VERIFICATION_NOT_COMPLETED))
                    .given(verificationService).validateConfirmed(EMAIL, VerificationType.SIGN_UP);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.VERIFICATION_NOT_COMPLETED);

            // 이메일 인증 실패 → DB 조회 및 저장 미호출
            then(userRepository).should(never()).existsByLoginId(anyString());
            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("중복 loginId → DUPLICATE_ID 예외를 던지고 저장하지 않는다")
        void duplicateLoginId_throwsDuplicateId() {
            // loginIdToken 검증 + 이메일 인증 모두 통과, DB에서 loginId 중복 발견
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(true);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.DUPLICATE_ID);

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("중복 email → DUPLICATE_EMAIL 예외를 던지고 저장하지 않는다")
        void duplicateEmail_throwsDuplicateEmail() {
            // loginId는 통과, email 중복
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(false);
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.DUPLICATE_EMAIL);

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("정상 회원가입 → 비밀번호 인코딩 후 저장하고 인증 상태를 소비한다")
        void validSignUp_savesUserAndConsumesConfirmation() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(dto.getPassword()).willReturn(UserFixture.RAW_PASSWORD);
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(false);
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(UserFixture.RAW_PASSWORD)).willReturn(ENCODED_PW);
            UserEntity newUser = UserFixture.localUser();
            given(userMapper.fromSignUpRequest(dto, ENCODED_PW)).willReturn(newUser);

            authService.signUp(dto);

            then(passwordEncoder).should(times(1)).encode(UserFixture.RAW_PASSWORD);
            then(userMapper).should(times(1)).fromSignUpRequest(dto, ENCODED_PW);
            then(userRepository).should(times(1)).save(newUser);
            // 저장 후 인증 상태 소비 — 동일 토큰으로 재가입 방지
            then(verificationService).should(times(1))
                    .consumeConfirmation(EMAIL, VerificationType.SIGN_UP);
        }

        @Test
        @DisplayName("정상 회원가입 → 검증 순서가 토큰→이메일인증→DB중복→저장→인증소비 임을 확인한다")
        void validSignUp_executesInCorrectOrder() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(dto.getPassword()).willReturn(UserFixture.RAW_PASSWORD);
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(false);
            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(UserFixture.RAW_PASSWORD)).willReturn(ENCODED_PW);
            given(userMapper.fromSignUpRequest(dto, ENCODED_PW)).willReturn(UserFixture.localUser());

            authService.signUp(dto);

            // InOrder: 호출 순서 위반 시 테스트 실패
            var inOrder = inOrder(loginIdTokenUtil, verificationService, userRepository);
            inOrder.verify(loginIdTokenUtil).validate(LOGIN_ID, LOGIN_ID_TOKEN);
            inOrder.verify(verificationService).validateConfirmed(EMAIL, VerificationType.SIGN_UP);
            inOrder.verify(userRepository).existsByLoginId(LOGIN_ID);
            inOrder.verify(userRepository).existsByEmail(EMAIL);
            inOrder.verify(userRepository).save(any());
            inOrder.verify(verificationService).consumeConfirmation(EMAIL, VerificationType.SIGN_UP);
        }
    }

    @Nested
    @DisplayName("reissue()")
    class Reissue {

        private void stubAuthenticateRt() {
            CustomUserDetails userDetails = new CustomUserDetails(USER_PK, ROLE, SESSION_ID);
            given(tokenService.authenticateRefreshToken(TokenFixture.FAKE_RT))
                    .willReturn(userDetails);
        }

        private void stubFullReissueFlow() {
            stubAuthenticateRt();
            given(deviceService.rotateDeviceSession(USER_PK, SESSION_ID))
                    .willReturn(NEW_SESSION_ID);
            given(tokenService.rotateTokens(USER_PK, ROLE, SESSION_ID, NEW_SESSION_ID))
                    .willReturn(TokenFixture.newTokenPair());
        }

        @Test
        @DisplayName("RT가 비어있으면 AUTHENTICATION_FAIL 예외를 던진다")
        void emptyRefreshToken_throwsAuthenticationFail() {
            assertThatThrownBy(() -> authService.reissue(""))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);

            then(tokenService).should(never()).authenticateRefreshToken(anyString());
        }

        @Test
        @DisplayName("유효한 AT + RT → 세션 롤링 후 새 토큰 쌍을 반환한다")
        void validTokens_returnsNewTokenPair() {
            stubFullReissueFlow();

            AuthToken result = authService.reissue(TokenFixture.FAKE_RT);

            assertThat(result.getAccessToken()).isEqualTo(TokenFixture.NEW_FAKE_AT);
            assertThat(result.getRefreshToken()).isEqualTo(TokenFixture.NEW_FAKE_RT);
        }

        @Test
        @DisplayName("유효한 AT + RT → rotateDeviceSession과 rotateTokens가 올바른 인자로 호출된다")
        void validTokens_callsRotateWithCorrectArgs() {
            stubFullReissueFlow();

            authService.reissue(TokenFixture.FAKE_RT);

            then(deviceService).should(times(1)).rotateDeviceSession(USER_PK, SESSION_ID);
            then(tokenService).should(times(1)).rotateTokens(USER_PK, ROLE, SESSION_ID, NEW_SESSION_ID);
        }

        @Test
        @DisplayName("RT가 Redis 값과 불일치 → validateRefreshToken이 AUTHENTICATION_FAIL을 던진다")
        void mismatchedRt_throwsAuthenticationFail() {
            // authenticateRefreshToken 성공, validateRefreshToken 실패 (탈취 의심)
            stubAuthenticateRt();
            willThrow(new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL))
                    .given(tokenService)
                    .validateRefreshToken(USER_PK, SESSION_ID, TokenFixture.FAKE_RT);

            assertThatThrownBy(() -> authService.reissue(TokenFixture.FAKE_RT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);

            // validate 실패 후 rotateDeviceSession/rotateTokens 미호출
            then(deviceService).should(never()).rotateDeviceSession(anyLong(), anyString());
            then(tokenService).should(never()).rotateTokens(any(), any(), any(), any());
        }

        @Test
        @DisplayName("rotateDeviceSession 실패 (세션 없음) → AUTHENTICATION_FAIL 예외가 전파된다")
        void rotateDeviceSession_fails_propagatesException() {
            // deviceService가 존재하지 않는 sessionId로 예외를 던지는 시나리오
            stubAuthenticateRt();
            given(deviceService.rotateDeviceSession(USER_PK, SESSION_ID))
                    .willThrow(new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL));

            assertThatThrownBy(() -> authService.reissue(TokenFixture.FAKE_RT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);

            // rotateDeviceSession 실패 → rotateTokens 미호출
            then(tokenService).should(never()).rotateTokens(any(), any(), any(), any());
        }
    }

}