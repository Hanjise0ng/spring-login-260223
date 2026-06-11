package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.credential.repository.CredentialRepository;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.auth.factory.UserFactory;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.event.UserSignedUpEvent;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.domain.user.service.TagGenerator;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.fixture.TokenFixture;
import com.han.back.fixture.UserFixture;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.util.LoginIdTokenUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl")
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CredentialRepository credentialRepository;
    @Mock private UserFactory userFactory;
    @Mock private TagGenerator tagGenerator;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenService tokenService;
    @Mock private DeviceService deviceService;
    @Mock private VerificationService verificationService;
    @Mock private LoginIdTokenUtil loginIdTokenUtil;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final Long USER_PK = 1L;
    private static final Role ROLE = Role.USER;
    private static final String LOGIN_ID = UserFixture.DEFAULT_LOGIN_ID;
    private static final String EMAIL = "test@test.com";
    private static final String LOGIN_ID_TOKEN = "valid.loginid.token";
    private static final String SESSION_ID = "session-abc-123";
    private static final String NEW_SESSION_ID = "new-session-xyz-456";
    private static final String ENCODED_PW = "$2a$10$encodedPasswordForTest";
    private static final Long EXISTING_USER_ID = 99L;

    @Nested
    @DisplayName("checkLoginId()")
    class CheckLoginId {

        @Test
        @DisplayName("이미 존재하는 loginId → DUPLICATE_LOGIN_ID 예외를 던지고 토큰을 발급하지 않는다")
        void existingLoginId_throwsDuplicateLoginId() {
            given(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                    .willReturn(true);

            assertThatThrownBy(() -> authService.checkLoginId(LOGIN_ID))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(AccountResponseStatus.ACCOUNT_DUPLICATE_LOGIN_ID);

            then(loginIdTokenUtil).should(never()).issue(anyString());
        }

        @Test
        @DisplayName("사용 가능한 loginId → loginIdToken이 담긴 LoginIdCheckResponseDto를 반환한다")
        void availableLoginId_returnsLoginIdCheckResponseDto() {
            given(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                    .willReturn(false);
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
        @DisplayName("loginIdToken 검증 실패: LOGIN_ID_CHECK_REQUIRED를 던지고 이후 로직을 실행하지 않는다")
        void invalidLoginIdToken_throwsLoginIdCheckRequired() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            willThrow(new CustomException(AuthResponseStatus.AUTH_LOGIN_ID_CHECK_REQUIRED))
                    .given(loginIdTokenUtil).validate(LOGIN_ID, LOGIN_ID_TOKEN);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(AuthResponseStatus.AUTH_LOGIN_ID_CHECK_REQUIRED);

            then(verificationService).should(never()).validateConfirmed(anyString(), any());
            then(credentialRepository).should(never()).existsByProviderAndIdentifier(any(), anyString());
            then(userRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("이메일 인증 미완료: VERIFY_NOT_COMPLETED를 던지고 DB 접근을 하지 않는다")
        void emailNotVerified_throwsVerificationNotCompleted() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            willThrow(new CustomException(VerificationResponseStatus.VERIFY_NOT_COMPLETED))
                    .given(verificationService).validateConfirmed(EMAIL, VerificationType.SIGN_UP);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_NOT_COMPLETED);

            then(credentialRepository).should(never()).existsByProviderAndIdentifier(any(), anyString());
            then(userRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("중복 loginId: DUPLICATE_LOGIN_ID 예외를 던지고 저장하지 않는다")
        void duplicateLoginId_throwsDuplicateLoginId() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                    .willReturn(true);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(AccountResponseStatus.ACCOUNT_DUPLICATE_LOGIN_ID);

            then(userRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("중복 email(LOCAL 계정 존재): DUPLICATE_EMAIL 예외를 던지고 저장하지 않는다")
        void duplicateEmail_throwsDuplicateEmail() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                    .willReturn(false);

            // existsLocalEmail 내부: findByEmail → getId → findByUserIdAndProvider(LOCAL)
            UserEntity existingUser = mock(UserEntity.class);
            given(existingUser.getId()).willReturn(EXISTING_USER_ID);
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.of(existingUser));
            given(credentialRepository.findByUserIdAndProvider(EXISTING_USER_ID, AuthProvider.LOCAL))
                    .willReturn(Optional.of(mock(CredentialEntity.class)));

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL);

            then(userRepository).should(never()).save(any());
            then(eventPublisher).should(never()).publishEvent(any());
        }

        @Test
        @DisplayName("정상 회원가입: user와 credential을 저장하고 이벤트를 발행한다")
        void validSignUp_savesUserAndCredentialAndPublishesEvent() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(dto.getPassword()).willReturn(UserFixture.RAW_PASSWORD);
            given(dto.getNickname()).willReturn(UserFixture.DEFAULT_NICKNAME);
            given(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                    .willReturn(false);
            // existsLocalEmail → false (이메일 없음)
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
            given(tagGenerator.generate(UserFixture.DEFAULT_NICKNAME)).willReturn("A1B2");

            UserEntity newUser = mock(UserEntity.class);
            given(newUser.getId()).willReturn(USER_PK);
            given(userFactory.createLocalUser(dto, "A1B2")).willReturn(newUser);
            given(passwordEncoder.encode(UserFixture.RAW_PASSWORD)).willReturn(ENCODED_PW);
            CredentialEntity newCredential = mock(CredentialEntity.class);
            given(userFactory.createLocalCredential(USER_PK, LOGIN_ID, ENCODED_PW))
                    .willReturn(newCredential);

            authService.signUp(dto);

            then(tagGenerator).should(times(1)).generate(UserFixture.DEFAULT_NICKNAME);
            then(passwordEncoder).should(times(1)).encode(UserFixture.RAW_PASSWORD);
            then(userFactory).should(times(1)).createLocalUser(dto, "A1B2");
            then(userRepository).should(times(1)).save(newUser);
            then(userFactory).should(times(1)).createLocalCredential(USER_PK, LOGIN_ID, ENCODED_PW);
            then(credentialRepository).should(times(1)).save(newCredential);

            ArgumentCaptor<UserSignedUpEvent> eventCaptor = ArgumentCaptor.forClass(UserSignedUpEvent.class);
            then(eventPublisher).should(times(1)).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue()).isNotNull();
        }

        @Test
        @DisplayName("정상 회원가입: 토큰검증 → 이메일인증 → loginId중복 → 이메일중복 → user저장 → credential저장 → 이벤트 순서로 실행된다")
        void validSignUp_executesInCorrectOrder() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getLoginIdToken()).willReturn(LOGIN_ID_TOKEN);
            given(dto.getEmail()).willReturn(EMAIL);
            given(dto.getPassword()).willReturn(UserFixture.RAW_PASSWORD);
            given(dto.getNickname()).willReturn(UserFixture.DEFAULT_NICKNAME);
            given(credentialRepository.existsByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID))
                    .willReturn(false);
            given(userRepository.findByEmail(EMAIL)).willReturn(Optional.empty());
            given(tagGenerator.generate(UserFixture.DEFAULT_NICKNAME)).willReturn("A1B2");

            UserEntity newUser = mock(UserEntity.class);
            given(newUser.getId()).willReturn(USER_PK);
            given(userFactory.createLocalUser(dto, "A1B2")).willReturn(newUser);
            given(passwordEncoder.encode(UserFixture.RAW_PASSWORD)).willReturn(ENCODED_PW);
            given(userFactory.createLocalCredential(USER_PK, LOGIN_ID, ENCODED_PW))
                    .willReturn(mock(CredentialEntity.class));

            authService.signUp(dto);

            var inOrder = inOrder(loginIdTokenUtil, verificationService, credentialRepository,
                    userRepository, tagGenerator, userFactory, eventPublisher);
            inOrder.verify(loginIdTokenUtil).validate(LOGIN_ID, LOGIN_ID_TOKEN);
            inOrder.verify(verificationService).validateConfirmed(EMAIL, VerificationType.SIGN_UP);
            inOrder.verify(credentialRepository).existsByProviderAndIdentifier(AuthProvider.LOCAL, LOGIN_ID);
            inOrder.verify(userRepository).findByEmail(EMAIL);
            inOrder.verify(tagGenerator).generate(UserFixture.DEFAULT_NICKNAME);
            inOrder.verify(userFactory).createLocalUser(dto, "A1B2");
            inOrder.verify(userRepository).save(newUser);
            inOrder.verify(userFactory).createLocalCredential(USER_PK, LOGIN_ID, ENCODED_PW);
            inOrder.verify(credentialRepository).save(any(CredentialEntity.class));
            inOrder.verify(eventPublisher).publishEvent(any(UserSignedUpEvent.class));
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
                    .isEqualTo(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL);

            then(tokenService).should(never()).authenticateRefreshToken(anyString());
        }

        @Test
        @DisplayName("유효한 RT: 세션 롤링 후 새 토큰 쌍을 반환한다")
        void validRefreshToken_returnsNewTokenPair() {
            stubFullReissueFlow();

            AuthToken result = authService.reissue(TokenFixture.FAKE_RT);

            assertThat(result.getAccessToken()).isEqualTo(TokenFixture.NEW_FAKE_AT);
            assertThat(result.getRefreshToken()).isEqualTo(TokenFixture.NEW_FAKE_RT);
        }

        @Test
        @DisplayName("유효한 RT: rotateDeviceSession과 rotateTokens가 올바른 인자로 호출된다")
        void validRefreshToken_callsRotateWithCorrectArgs() {
            stubFullReissueFlow();

            authService.reissue(TokenFixture.FAKE_RT);

            then(deviceService).should(times(1)).rotateDeviceSession(USER_PK, SESSION_ID);
            then(tokenService).should(times(1)).rotateTokens(USER_PK, ROLE, SESSION_ID, NEW_SESSION_ID);
        }

        @Test
        @DisplayName("RT가 Redis 값과 불일치: validateRefreshToken이 AUTHENTICATION_FAIL을 던진다")
        void mismatchedRt_throwsAuthenticationFail() {
            stubAuthenticateRt();
            willThrow(new CustomAuthenticationException(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL))
                    .given(tokenService)
                    .validateRefreshToken(USER_PK, SESSION_ID, TokenFixture.FAKE_RT);

            assertThatThrownBy(() -> authService.reissue(TokenFixture.FAKE_RT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL);

            then(deviceService).should(never()).rotateDeviceSession(anyLong(), anyString());
            then(tokenService).should(never()).rotateTokens(any(), any(), any(), any());
        }

        @Test
        @DisplayName("rotateDeviceSession 실패 (세션 없음): AUTHENTICATION_FAIL 예외가 전파된다")
        void rotateDeviceSession_fails_propagatesException() {
            stubAuthenticateRt();
            given(deviceService.rotateDeviceSession(USER_PK, SESSION_ID))
                    .willThrow(new CustomAuthenticationException(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL));

            assertThatThrownBy(() -> authService.reissue(TokenFixture.FAKE_RT))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(AuthResponseStatus.AUTH_AUTHENTICATION_FAIL);

            then(tokenService).should(never()).rotateTokens(any(), any(), any(), any());
        }
    }

}