package com.han.back.domain.verification.service.implement;

import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import com.han.back.global.notification.NotificationChannel;
import com.han.back.global.notification.NotificationSender;
import com.han.back.global.notification.template.MailTemplateUtil;
import com.han.back.global.security.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @Mock private MailTemplateUtil mailTemplateUtil;
    @Mock private NotificationSender emailSender;
    @Mock private VerificationPolicy signUpPolicy;
    @Mock private VerificationPolicy passwordResetPolicy;

    @Captor private ArgumentCaptor<String> codeCaptor;

    private VerificationServiceImpl verificationService;

    private static final String EMAIL          = "test@example.com";
    private static final String SHORT_EMAIL    = "ab@example.com";
    private static final String PHONE          = "01012345678";
    private static final String SHORT_TARGET   = "abc";
    private static final String HTML_CONTENT   = "<html>code</html>";

    @BeforeEach
    void setUp() {
        given(emailSender.getChannel()).willReturn(NotificationChannel.EMAIL);
        given(signUpPolicy.getSupportedTypes()).willReturn(Set.of(VerificationType.SIGN_UP));
        given(passwordResetPolicy.getSupportedTypes()).willReturn(Set.of(VerificationType.PASSWORD_RESET));

        verificationService = new VerificationServiceImpl(
                redisUtil, mailTemplateUtil,
                List.of(emailSender),
                List.of(signUpPolicy, passwordResetPolicy)
        );
    }

    private VerificationSendRequestDto sendRequest(String target, VerificationType type, NotificationChannel channel) {
        VerificationSendRequestDto dto = mock(VerificationSendRequestDto.class);
        given(dto.getTarget()).willReturn(target);
        given(dto.getType()).willReturn(type);
        given(dto.getChannel()).willReturn(channel);
        return dto;
    }

    private VerificationConfirmRequestDto confirmRequest(String target, VerificationType type, String code) {
        VerificationConfirmRequestDto dto = mock(VerificationConfirmRequestDto.class);
        given(dto.getTarget()).willReturn(target);
        given(dto.getType()).willReturn(type);
        given(dto.getCode()).willReturn(code);
        return dto;
    }

    private void stubSendCodeHappyPath() {
        given(redisUtil.hasKey(anyString())).willReturn(false);
        given(mailTemplateUtil.buildVerificationEmail(anyString(), anyString(), anyLong()))
                .willReturn(HTML_CONTENT);
    }

    @Nested
    @DisplayName("sendCode()")
    class SendCode {

        @Test
        @DisplayName("정상 요청 → 인증 코드를 생성·저장하고 이메일을 발송한 뒤 TTL 정보를 초 단위로 반환한다")
        void happyPath_sendsCodeAndReturnsTtl() {
            stubSendCodeHappyPath();
            VerificationSendRequestDto request = sendRequest(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL);

            VerificationSendResponseDto result = verificationService.sendCode(request);

            assertThat(result.getCodeExpiresIn()).isEqualTo(VerificationConst.CODE_TTL / 1_000);
            assertThat(result.getCooldownExpiresIn()).isEqualTo(VerificationConst.COOLDOWN_TTL / 1_000);
        }

        @Test
        @DisplayName("정상 요청 → 정책 검증 → 쿨다운 확인 → 코드 저장 → 발송 순서로 실행된다")
        void happyPath_executesInCorrectOrder() {
            stubSendCodeHappyPath();
            VerificationSendRequestDto request = sendRequest(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL);

            verificationService.sendCode(request);

            var inOrder = inOrder(signUpPolicy, redisUtil, mailTemplateUtil, emailSender);

            // 정책 검증
            inOrder.verify(signUpPolicy).check(EMAIL);
            // 쿨다운 확인 (cooldownKey로 hasKey)
            inOrder.verify(redisUtil).hasKey(VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL));
            // 코드 저장 (codeKey로 setDataExpire)
            inOrder.verify(redisUtil).setDataExpire(
                    eq(VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL)), anyString(), eq(VerificationConst.CODE_TTL));
            // 쿨다운 저장
            inOrder.verify(redisUtil).setDataExpire(
                    eq(VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL)), eq("ACTIVE"), eq(VerificationConst.COOLDOWN_TTL));
            // 이메일 콘텐츠 생성
            inOrder.verify(mailTemplateUtil).buildVerificationEmail(anyString(), anyString(), anyLong());
            // 발송
            inOrder.verify(emailSender).send(eq(EMAIL), eq(VerificationType.SIGN_UP.getEmailSubject()), eq(HTML_CONTENT));
        }

        @Test
        @DisplayName("정상 요청 → 생성된 코드가 지정된 자릿수의 숫자 문자열이다")
        void happyPath_generatesCodeWithCorrectLength() {
            stubSendCodeHappyPath();
            VerificationSendRequestDto request = sendRequest(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL);

            verificationService.sendCode(request);

            then(redisUtil).should().setDataExpire(
                    eq(VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL)),
                    codeCaptor.capture(),
                    eq(VerificationConst.CODE_TTL)
            );
            String capturedCode = codeCaptor.getValue();
            assertThat(capturedCode)
                    .hasSize(VerificationConst.CODE_LENGTH)
                    .containsOnlyDigits();
        }

        @Test
        @DisplayName("정책이 매핑되지 않은 타입 → 정책 검증을 건너뛰고 정상 발송한다")
        void unmappedPolicyType_skipsCheckAndSendsCode() {
            stubSendCodeHappyPath();
            // EMAIL_CHANGE 타입은 setUp에서 policyMap에 등록하지 않음
            VerificationSendRequestDto request = sendRequest(EMAIL, VerificationType.EMAIL_CHANGE, NotificationChannel.EMAIL);

            assertThatCode(() -> verificationService.sendCode(request))
                    .doesNotThrowAnyException();

            then(signUpPolicy).should(never()).check(anyString());
            then(passwordResetPolicy).should(never()).check(anyString());
            then(emailSender).should(times(1)).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("정책 검증 실패 → 예외가 그대로 전파되고 코드를 저장하지 않는다")
        void policyCheckFails_propagatesExceptionWithoutStoringCode() {
            willThrow(new CustomException(BaseResponseStatus.DUPLICATE_EMAIL))
                    .given(signUpPolicy).check(EMAIL);
            VerificationSendRequestDto request = sendRequest(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL);

            assertThatThrownBy(() -> verificationService.sendCode(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.DUPLICATE_EMAIL);

            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), anyLong());
            then(emailSender).should(never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("쿨다운 활성 상태 → COOLDOWN_ACTIVE 예외를 던지고 코드를 발송하지 않는다")
        void cooldownActive_throwsCooldownActive() {
            given(redisUtil.hasKey(VerificationConst.cooldownKey(VerificationType.SIGN_UP, EMAIL)))
                    .willReturn(true);
            VerificationSendRequestDto request = sendRequest(EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL);

            assertThatThrownBy(() -> verificationService.sendCode(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.COOLDOWN_ACTIVE);

            then(emailSender).should(never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("이메일 로컬파트 2자 이하 → maskTarget이 '***@domain' 형태로 마스킹한다 (atIndex <= 2 분기)")
        void shortEmailLocal_masksCorrectly() {
            stubSendCodeHappyPath();
            VerificationSendRequestDto request = sendRequest(SHORT_EMAIL, VerificationType.SIGN_UP, NotificationChannel.EMAIL);

            // maskTarget은 log.info 내부에서 호출되므로, 예외 없이 완료되면 해당 분기가 커버된다
            assertThatCode(() -> verificationService.sendCode(request))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("미지원 채널(SMS) → UNSUPPORTED_NOTIFICATION_CHANNEL 예외를 던지고 이후 로직을 실행하지 않는다")
        void unsupportedChannel_throwsUnsupportedNotificationChannel() {
            VerificationSendRequestDto request = sendRequest(PHONE, VerificationType.SIGN_UP, NotificationChannel.SMS);

            assertThatThrownBy(() -> verificationService.sendCode(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.UNSUPPORTED_NOTIFICATION_CHANNEL);

            then(redisUtil).should(never()).hasKey(anyString());
            then(redisUtil).should(never()).setDataExpire(anyString(), anyString(), anyLong());
            then(emailSender).should(never()).send(anyString(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("confirmCode()")
    class ConfirmCode {

        @Test
        @DisplayName("정상 인증 → 코드 키를 삭제하고 confirmed 상태를 저장한다")
        void validCode_deletesCodeAndStoresConfirmed() {
            String code = "123456";
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            String confirmedKey = VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of(code));

            VerificationConfirmRequestDto request = confirmRequest(EMAIL, VerificationType.SIGN_UP, code);

            verificationService.confirmCode(request);

            var inOrder = inOrder(redisUtil);
            inOrder.verify(redisUtil).deleteData(codeKey);
            inOrder.verify(redisUtil).setDataExpire(confirmedKey, "CONFIRMED", VerificationConst.CONFIRMED_TTL);
        }

        @Test
        @DisplayName("코드 만료 (Redis에 없음) → VERIFICATION_EXPIRED 예외를 던진다")
        void expiredCode_throwsVerificationExpired() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.empty());

            VerificationConfirmRequestDto request = confirmRequest(EMAIL, VerificationType.SIGN_UP, "123456");

            assertThatThrownBy(() -> verificationService.confirmCode(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.VERIFICATION_EXPIRED);

            then(redisUtil).should(never()).deleteData(anyString());
        }

        @Test
        @DisplayName("코드 불일치 → VERIFICATION_FAIL 예외를 던지고 기존 코드를 삭제하지 않는다")
        void wrongCode_throwsVerificationFail() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, EMAIL);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of("999999"));

            VerificationConfirmRequestDto request = confirmRequest(EMAIL, VerificationType.SIGN_UP, "000000");

            assertThatThrownBy(() -> verificationService.confirmCode(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.VERIFICATION_FAIL);

            then(redisUtil).should(never()).deleteData(anyString());
        }

        @Test
        @DisplayName("짧은 타겟(길이 ≤ 4, @없음) → maskTarget이 '****'로 마스킹한다")
        void shortTarget_masksToStars() {
            String codeKey = VerificationConst.codeKey(VerificationType.SIGN_UP, SHORT_TARGET);
            given(redisUtil.getData(codeKey)).willReturn(Optional.of("123456"));

            VerificationConfirmRequestDto request = confirmRequest(SHORT_TARGET, VerificationType.SIGN_UP, "123456");

            assertThatCode(() -> verificationService.confirmCode(request))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateConfirmed()")
    class ValidateConfirmed {

        @Test
        @DisplayName("confirmed 키 존재 → 예외 없이 통과한다")
        void confirmedExists_passes() {
            given(redisUtil.hasKey(VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL)))
                    .willReturn(true);

            assertThatCode(() -> verificationService.validateConfirmed(EMAIL, VerificationType.SIGN_UP))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("confirmed 키 미존재 → VERIFICATION_NOT_COMPLETED 예외를 던진다")
        void confirmedNotExists_throwsNotCompleted() {
            given(redisUtil.hasKey(VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL)))
                    .willReturn(false);

            assertThatThrownBy(() -> verificationService.validateConfirmed(EMAIL, VerificationType.SIGN_UP))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(BaseResponseStatus.VERIFICATION_NOT_COMPLETED);
        }
    }

    @Nested
    @DisplayName("consumeConfirmation()")
    class ConsumeConfirmation {

        @Test
        @DisplayName("confirmed 키를 삭제하여 재사용을 방지한다")
        void deletesConfirmedKey() {
            verificationService.consumeConfirmation(EMAIL, VerificationType.SIGN_UP);

            then(redisUtil).should(times(1))
                    .deleteData(VerificationConst.confirmedKey(VerificationType.SIGN_UP, EMAIL));
        }
    }

}