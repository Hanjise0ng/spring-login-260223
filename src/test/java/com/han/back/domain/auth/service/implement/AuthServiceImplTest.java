package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.device.service.DeviceService;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.mapper.UserMapper;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.fixture.TokenFixture;
import com.han.back.fixture.UserFixture;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.dto.CustomUserDetails;
import com.han.back.global.security.service.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;
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

    @InjectMocks private AuthServiceImpl authService;

    private static final Long   USER_PK        = 1L;
    private static final Role   ROLE           = Role.USER;
    private static final String LOGIN_ID       = UserFixture.DEFAULT_LOGIN_ID;
    private static final String SESSION_ID     = "session-abc-123";
    private static final String NEW_SESSION_ID = "new-session-xyz-456";
    private static final String ENCODED_PW     = "$2a$10$encodedPasswordForTest";

    @Nested
    @DisplayName("signUp()")
    class SignUp {

        @Test
        @DisplayName("중복 loginId → DUPLICATE_ID 예외를 던진다")
        void duplicateLoginId_throwsDuplicateId() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(true);

            assertThatThrownBy(() -> authService.signUp(dto))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.DUPLICATE_ID);

            // 중복 확인 후 즉시 throw → save 미호출
            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("신규 loginId → 비밀번호 인코딩 후 UserEntity를 저장한다")
        void newLoginId_savesUser() {
            SignUpRequestDto dto = mock(SignUpRequestDto.class);
            given(dto.getLoginId()).willReturn(LOGIN_ID);
            given(dto.getPassword()).willReturn(UserFixture.RAW_PASSWORD);
            given(userRepository.existsByLoginId(LOGIN_ID)).willReturn(false);
            given(passwordEncoder.encode(UserFixture.RAW_PASSWORD)).willReturn(ENCODED_PW);

            UserEntity newUser = UserFixture.localUser();
            given(userMapper.fromSignUpRequest(dto, ENCODED_PW)).willReturn(newUser);

            authService.signUp(dto);

            then(passwordEncoder).should(times(1)).encode(UserFixture.RAW_PASSWORD);
            then(userMapper).should(times(1)).fromSignUpRequest(dto, ENCODED_PW);
            then(userRepository).should(times(1)).save(newUser);
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
            AuthTokenDto emptyRt = AuthTokenDto.of(TokenFixture.FAKE_AT, "");

            assertThatThrownBy(() -> authService.reissue(emptyRt))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);

            then(tokenService).should(never()).authenticateRefreshToken(anyString());
        }

        @Test
        @DisplayName("유효한 AT + RT → 세션 롤링 후 새 토큰 쌍을 반환한다")
        void validTokens_returnsNewTokenPair() {
            stubFullReissueFlow();

            AuthTokenDto result = authService.reissue(TokenFixture.tokenPair());

            assertThat(result.getAccessToken()).isEqualTo(TokenFixture.NEW_FAKE_AT);
            assertThat(result.getRefreshToken()).isEqualTo(TokenFixture.NEW_FAKE_RT);
        }

        @Test
        @DisplayName("유효한 AT + RT → rotateDeviceSession과 rotateTokens가 올바른 인자로 호출된다")
        void validTokens_callsRotateWithCorrectArgs() {
            stubFullReissueFlow();

            authService.reissue(TokenFixture.tokenPair());

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

            assertThatThrownBy(() -> authService.reissue(TokenFixture.tokenPair()))
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

            assertThatThrownBy(() -> authService.reissue(TokenFixture.tokenPair()))
                    .isInstanceOf(CustomAuthenticationException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.AUTHENTICATION_FAIL);

            // rotateDeviceSession 실패 → rotateTokens 미호출
            then(tokenService).should(never()).rotateTokens(any(), any(), any(), any());
        }
    }

}