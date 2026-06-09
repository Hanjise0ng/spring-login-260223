package com.han.back.domain.verification.service.implement;

import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.dispatcher.NotificationDispatcher;
import com.han.back.global.infra.notification.model.NotificationChannel;
import com.han.back.global.infra.notification.model.NotificationCommand;
import com.han.back.global.infra.notification.model.NotificationPurpose;
import com.han.back.global.infra.notification.policy.NotificationKeyPolicy;
import com.han.back.global.infra.notification.template.MailTemplateUtil;
import com.han.back.global.infra.redis.util.RateLimitUtil;
import com.han.back.global.infra.redis.util.RedisUtil;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.util.RateLimitConst;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VerificationServiceImpl")
class VerificationServiceImplTest {

    @Mock private RedisUtil redisUtil;
    @Mock private RateLimitUtil rateLimitUtil;
    @Mock private MailTemplateUtil mailTemplateUtil;
    @Mock private NotificationDispatcher notificationDispatcher;
    @Mock private NotificationKeyPolicy keyPolicy;
    @Mock private VerificationPolicy signUpPolicy;
    @Mock private VerificationPolicy passwordResetPolicy;

    @Captor private ArgumentCaptor<String> codeCaptor;
    @Captor private ArgumentCaptor<NotificationCommand> commandCaptor;

    private VerificationServiceImpl verificationService;

    private static final String EMAIL = "test@example.com";
    private static final String PHONE = "01012345678";
    private static final String HTML_CONTENT = "<html>code</html>";

    private static final String DEDUPE_KEY =
            "verification:" + VerificationType.SIGN_UP.name() + ":" + EMAIL;

    @BeforeEach
    void setUp() {
        given(signUpPolicy.getSupportedTypes())
                .willReturn(Set.of(VerificationType.SIGN_UP));
        given(passwordResetPolicy.getSupportedTypes())
                .willReturn(Set.of(VerificationType.PASSWORD_RESET));

        verificationService = new VerificationServiceImpl(
                redisUtil, rateLimitUtil,
                mailTemplateUtil,
                notificationDispatcher,
                keyPolicy,
                List.of(signUpPolicy, passwordResetPolicy)
        );
    }

    /**
     * 쿨다운 통과 + 시간당 제한 통과 stub.
     * - hasKey(cooldownKey) → false
     * - rateLimitUtil.incrementHourly(any) → 1L (첫 번째 발송)
     */
    private void stubSendCodeBeforeDispatch() {
        given(redisUtil.hasKey(anyString())).willReturn(false);
        given(rateLimitUtil.incrementHourly(anyString())).willReturn(1L);
    }

    /** dispatch 직전 전체 경로 — stubSendCodeBeforeDispatch 포함 */
    private void stubSendCodeToDispatch() {
        stubSendCodeBeforeDispatch();
        given(mailTemplateUtil.buildVerificationEmail(anyString(), anyString(), anyLong()))
                .willReturn(HTML_CONTENT);

        given(keyPolicy.verification(anyString(), anyString())).willReturn(DEDUPE_KEY);
    }

    @Nested
    @DisplayName("sendCode()")
    class SendCode {

        @Test
        @DisplayName("정상 요청 → TTL 응답 반환 + Dispatcher 1회 호출")
        void happyPath_returnsTtlAndDispatches() {
            stubSendCodeToDispatch();

            VerificationSendResponseDto result = verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            );

            assertThat(result.getCodeExpiresIn()).isEqualTo(VerificationConst.CODE_TTL.toSeconds());
            assertThat(result.getCooldownExpiresIn()).isEqualTo(VerificationConst.COOLDOWN_TTL.toSeconds());
            then(notificationDispatcher).should(times(1)).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("정상 요청 → 보안 검증 순서: 채널 → 정책 → 쿨다운 → 시간제한 → 저장 → 발송")
        void happyPath_executesInSecurityOrder() {
            stubSendCodeToDispatch();

            verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            );

            var inOrder = inOrder(signUpPolicy, redisUtil, rateLimitUtil, mailTemplateUtil, notificationDispatcher);
            inOrder.verify(signUpPolicy).check(EMAIL, NotificationChannel.EMAIL);
            inOrder.verify(redisUtil).hasKey(
                    VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL));
            inOrder.verify(rateLimitUtil).incrementHourly(anyString());
            inOrder.verify(redisUtil).setDataExpire(
                    eq(VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL)),
                    anyString(), eq(VerificationConst.CODE_TTL));
            inOrder.verify(redisUtil).setDataExpire(
                    eq(VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL)),
                    eq("ACTIVE"), eq(VerificationConst.COOLDOWN_TTL));
            inOrder.verify(mailTemplateUtil).buildVerificationEmail(anyString(), anyString(), anyLong());
            inOrder.verify(notificationDispatcher).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("정상 요청 → NotificationCommand — request/metadata 필드가 올바르게 조립된다")
        void happyPath_commandAssembledCorrectly() {
            stubSendCodeToDispatch();

            verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            );

            then(notificationDispatcher).should().dispatch(commandCaptor.capture());
            NotificationCommand command = commandCaptor.getValue();

            assertThat(command.getRequest().getChannel()).isEqualTo(NotificationChannel.EMAIL);
            assertThat(command.getRequest().getTarget()).isEqualTo(EMAIL);
            assertThat(command.getRequest().getPurpose()).isEqualTo(NotificationPurpose.VERIFICATION);
            assertThat(command.getRequest().getSubject())
                    .isEqualTo(VerificationType.SIGN_UP.getEmailSubject());
            assertThat(command.getRequest().getContent()).isEqualTo(HTML_CONTENT);

            assertThat(command.getMetadata().getDedupeKey()).isEqualTo(DEDUPE_KEY);
            assertThat(command.getMetadata().getTraceKey()).isNotBlank();
        }

        @Test
        @DisplayName("정상 요청 → 생성 코드는 6자리 숫자 문자열")
        void happyPath_codeIsNumericAndCorrectLength() {
            stubSendCodeToDispatch();

            verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            );

            then(redisUtil).should().setDataExpire(
                    eq(VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL)),
                    codeCaptor.capture(),
                    eq(VerificationConst.CODE_TTL)
            );
            assertThat(codeCaptor.getValue())
                    .hasSize(VerificationConst.CODE_LENGTH)
                    .containsOnlyDigits();
        }

        @Test
        @DisplayName("정책 미등록 타입(EMAIL_CHANGE) → 정책 체크 없이 발송한다")
        void unmappedPolicy_skipsCheckAndDispatches() {
            stubSendCodeToDispatch();

            assertThatCode(() -> verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.EMAIL_CHANGE, NotificationChannel.EMAIL)
            )).doesNotThrowAnyException();

            then(signUpPolicy).should(never()).check(anyString(), any());
            then(passwordResetPolicy).should(never()).check(anyString(), any());
            then(notificationDispatcher).should(times(1)).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("미지원 채널(SMS) → UNSUPPORTED_NOTIFICATION_CHANNEL, 이후 로직 전혀 실행 안 됨")
        void unsupportedChannel_blocksAllSubsequentLogic() {
            assertThatThrownBy(() -> verificationService.sendCode(
                    new VerificationSendRequestDto(PHONE, VerificationType.SIGN_UP, NotificationChannel.SMS)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_CHANNEL_UNSUPPORTED);

            then(redisUtil).should(never()).hasKey(anyString());
            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), any(Duration.class));
            then(rateLimitUtil).should(never()).incrementHourly(anyString());
            then(notificationDispatcher).should(never()).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("정책 검증 실패 → 예외 원본 전파, 코드 저장/발송 없음")
        void policyCheckFails_propagatesWithNoSideEffects() {
            willThrow(new CustomException(AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL))
                    .given(signUpPolicy).check(EMAIL, NotificationChannel.EMAIL);

            assertThatThrownBy(() -> verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(AccountResponseStatus.ACCOUNT_DUPLICATE_EMAIL);

            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), any(Duration.class));
            then(rateLimitUtil).should(never()).incrementHourly(anyString());
            then(notificationDispatcher).should(never()).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("쿨다운 활성 → COOLDOWN_ACTIVE, 코드 저장/발송 없음")
        void cooldownActive_blocksCodeGenerationAndDispatch() {
            given(redisUtil.hasKey(
                    VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL)))
                    .willReturn(true);

            assertThatThrownBy(() -> verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_COOLDOWN);

            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), any(Duration.class));
            then(rateLimitUtil).should(never()).incrementHourly(anyString());
            then(notificationDispatcher).should(never()).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("시간당 한도 초과 → RATE_LIMIT_EXCEEDED, 코드 저장/발송 없음")
        void hourlyLimitExceeded_blocksDispatch() {
            // 쿨다운은 없고 incrementHourly 결과가 한도(+1)를 초과
            given(redisUtil.hasKey(anyString())).willReturn(false);
            given(rateLimitUtil.incrementHourly(anyString()))
                    .willReturn((long) RateLimitConst.VERIFY_SEND_HOURLY_MAX + 1);

            assertThatThrownBy(() -> verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(ResponseStatus.RATE_LIMIT_EXCEEDED);

            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), any(Duration.class));
            then(notificationDispatcher).should(never()).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("시간당 정확히 한도 → 발송 허용 (경계값: max 이하)")
        void hourlyLimitAtMax_isAllowed() {
            given(redisUtil.hasKey(anyString())).willReturn(false);
            given(rateLimitUtil.incrementHourly(anyString()))
                    .willReturn((long) RateLimitConst.VERIFY_SEND_HOURLY_MAX);
            given(mailTemplateUtil.buildVerificationEmail(anyString(), anyString(), anyLong()))
                    .willReturn(HTML_CONTENT);
            given(keyPolicy.verification(anyString(), anyString())).willReturn(DEDUPE_KEY);

            assertThatCode(() -> verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            )).doesNotThrowAnyException();

            then(notificationDispatcher).should(times(1)).dispatch(any(NotificationCommand.class));
        }

        @Test
        @DisplayName("incrementHourly키 prefix에 타입과 target이 포함된다")
        void hourlyLimitKey_containsTypeAndTarget() {
            stubSendCodeToDispatch();

            verificationService.sendCode(
                    new VerificationSendRequestDto(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL)
            );

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(rateLimitUtil).should().incrementHourly(keyCaptor.capture());

            String capturedKey = keyCaptor.getValue();
            assertThat(capturedKey)
                    .contains(VerificationType.SIGN_UP.name())
                    .contains(EMAIL);
        }

    }

    @Nested
    @DisplayName("confirmCode()")
    class ConfirmCode {

        private static final String VALID_CODE  = "123456";
        private static final String WRONG_CODE  = "000000";
        private static final String STORED_CODE = "999999";

        @Test
        @DisplayName("정상 인증 → deleteData 후 setDataExpire (순서 역전 시 confirmed 유실 버그)")
        void validCode_deleteThenConfirmInOrder() {
            String codeKey      = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            String confirmedKey = VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(VALID_CODE));

            verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, VALID_CODE, VerificationType.SIGN_UP)
            );

            var inOrder = inOrder(redisUtil);
            inOrder.verify(redisUtil).deleteData(codeKey);
            inOrder.verify(redisUtil).setDataExpire(
                    confirmedKey, "CONFIRMED", VerificationConst.CONFIRMED_TTL);
        }

        @Test
        @DisplayName("정상 인증 → 실패 카운터 리셋")
        void validCode_resetsFailCounter() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(VALID_CODE));

            verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, VALID_CODE, VerificationType.SIGN_UP)
            );

            String expectedFailKey = RateLimitConst.VERIFY_FAIL_PREFIX
                    + VerificationType.SIGN_UP.name() + ":" + EMAIL;
            then(rateLimitUtil).should(times(1)).reset(expectedFailKey);
        }

        @Test
        @DisplayName("코드 만료 (Redis 키 없음) → VERIFICATION_EXPIRED, 삭제 없음")
        void expiredCode_throwsExpiredWithNoDeletion() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.empty());

            assertThatThrownBy(() -> verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, VALID_CODE, VerificationType.SIGN_UP)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_CODE_EXPIRED);

            then(redisUtil).should(never()).deleteData(anyString());
        }

        @Test
        @DisplayName("코드 불일치 → VERIFICATION_FAIL, 기존 코드 삭제 없음 (재시도 가능 상태 유지)")
        void wrongCode_throwsFailWithCodePreserved() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(STORED_CODE));
            given(rateLimitUtil.increment(anyString(), any(Duration.class)))
                    .willReturn(1L); // 첫 번째 실패 → 아직 한도 미달

            assertThatThrownBy(() -> verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, WRONG_CODE, VerificationType.SIGN_UP)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_CODE_MISMATCH);

            then(redisUtil).should(never()).deleteData(anyString());
        }

        @Test
        @DisplayName("코드 일치 시 confirmed 키의 TTL이 CONFIRMED_TTL로 설정된다")
        void validCode_confirmedTtlIsCorrect() {
            String codeKey      = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            String confirmedKey = VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(VALID_CODE));

            verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, VALID_CODE, VerificationType.SIGN_UP)
            );

            then(redisUtil).should().setDataExpire(
                    confirmedKey, "CONFIRMED", VerificationConst.CONFIRMED_TTL);
        }

        @Test
        @DisplayName("실패 4회 → VERIFICATION_FAIL, 코드 유지 (한도 미달)")
        void failFourTimes_codePreservedAndThrowsFail() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(STORED_CODE));
            // 4번째 실패 → VERIFY_FAIL_MAX - 1 이하
            given(rateLimitUtil.increment(anyString(), any(Duration.class)))
                    .willReturn((long) RateLimitConst.VERIFY_FAIL_MAX - 1);

            assertThatThrownBy(() -> verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, WRONG_CODE, VerificationType.SIGN_UP)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_CODE_MISMATCH);

            // 코드 삭제 없음
            then(redisUtil).should(never()).deleteData(anyString());
            // 실패 카운터 리셋 없음
            then(rateLimitUtil).should(never()).reset(anyString());
        }

        @Test
        @DisplayName("실패 5회(한도 도달) → VERIFICATION_EXPIRED, 코드 삭제 + 실패 카운터 리셋")
        void failFiveTimes_codeInvalidatedAndCounterReset() {
            String codeKey   = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            String failKey   = RateLimitConst.VERIFY_FAIL_PREFIX
                    + VerificationType.SIGN_UP.name() + ":" + EMAIL;

            given(redisUtil.getData(codeKey)).willReturn(Optional.of(STORED_CODE));
            // 5번째 실패 → VERIFY_FAIL_MAX 이상
            given(rateLimitUtil.increment(anyString(), any(Duration.class)))
                    .willReturn((long) RateLimitConst.VERIFY_FAIL_MAX);

            assertThatThrownBy(() -> verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, WRONG_CODE, VerificationType.SIGN_UP)
            ))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(	VerificationResponseStatus.VERIFY_CODE_EXPIRED);

            // 코드 무효화 (codeKey 삭제)
            then(redisUtil).should(times(1)).deleteData(codeKey);
            // 실패 카운터 리셋
            then(rateLimitUtil).should(times(1)).reset(failKey);
        }

        @Test
        @DisplayName("5회 실패 → increment 호출 시 정확한 failKey 사용")
        void failAttempt_usesCorrectFailKey() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(STORED_CODE));
            given(rateLimitUtil.increment(anyString(), any(Duration.class))).willReturn(1L);

            assertThatThrownBy(() -> verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, WRONG_CODE, VerificationType.SIGN_UP)
            )).isInstanceOf(CustomException.class);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            then(rateLimitUtil).should().increment(keyCaptor.capture(), any(Duration.class));

            assertThat(keyCaptor.getValue())
                    .startsWith(RateLimitConst.VERIFY_FAIL_PREFIX)
                    .contains(VerificationType.SIGN_UP.name())
                    .contains(EMAIL);
        }

        @Test
        @DisplayName("5회 초과 실패 → increment에 CODE_TTL이 전달된다 (카운터는 코드와 함께 만료)")
        void failAttempt_incrementCalledWithCodeTtl() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(STORED_CODE));
            given(rateLimitUtil.increment(anyString(), any(Duration.class))).willReturn(1L);

            assertThatThrownBy(() -> verificationService.confirmCode(
                    new VerificationConfirmRequestDto(EMAIL, WRONG_CODE, VerificationType.SIGN_UP)
            )).isInstanceOf(CustomException.class);

            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
            then(rateLimitUtil).should().increment(anyString(), ttlCaptor.capture());

            assertThat(ttlCaptor.getValue()).isEqualTo(VerificationConst.CODE_TTL);
        }
    }

    @Nested
    @DisplayName("validateConfirmed()")
    class ValidateConfirmed {

        @Test
        @DisplayName("confirmed 키 존재 → 예외 없이 통과")
        void confirmedExists_passes() {
            given(redisUtil.hasKey(VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL)))
                    .willReturn(true);

            assertThatCode(() ->
                    verificationService.validateConfirmed(EMAIL, VerificationType.SIGN_UP)
            ).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("confirmed 키 미존재 → VERIFICATION_NOT_COMPLETED")
        void confirmedNotExists_throwsNotCompleted() {
            given(redisUtil.hasKey(VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL)))
                    .willReturn(false);

            assertThatThrownBy(() ->
                    verificationService.validateConfirmed(EMAIL, VerificationType.SIGN_UP)
            )
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(VerificationResponseStatus.VERIFY_NOT_COMPLETED);
        }
    }

    @Nested
    @DisplayName("consumeConfirmation()")
    class ConsumeConfirmation {

        @Test
        @DisplayName("confirmed 키를 정확한 Redis 키로 삭제 → 재사용 방지")
        void deletesExactConfirmedKey() {
            verificationService.consumeConfirmation(EMAIL, VerificationType.SIGN_UP);

            then(redisUtil).should(times(1))
                    .deleteData(
                            eq(VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL)));
        }

        @Test
        @DisplayName("confirmed 키 없어도 예외 없이 종료 — 멱등 삭제 계약")
        void missingKey_isIdempotent() {
            //   UserSignedUpEventHandler는 가입 완료 후 consumeConfirmation을 호출함
            //   Redis TTL 만료로 키가 이미 없을 수 있음 → 예외 나면 환영 메일도 중단될 수 있음
            //   → 멱등성이 이 서비스의 명시적 계약임을 테스트로 고정
            assertThatCode(() ->
                    verificationService.consumeConfirmation("ghost@example.com", VerificationType.SIGN_UP)
            ).doesNotThrowAnyException();
        }
    }

}