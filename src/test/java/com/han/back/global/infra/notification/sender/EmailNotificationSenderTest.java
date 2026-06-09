package com.han.back.global.infra.notification.sender;

import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.model.NotificationChannel;
import com.han.back.global.infra.notification.model.NotificationPurpose;
import com.han.back.global.infra.notification.model.NotificationRequest;
import com.han.back.global.infra.notification.strategy.MailSendStrategy;
import com.han.back.global.response.ResponseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailNotificationSender")
class EmailNotificationSenderTest {

    private MailSendStrategy verificationStrategy;
    private MailSendStrategy welcomeStrategy;
    private MailSendStrategy newDeviceLoginStrategy;
    private EmailNotificationSender senderWithAllStrategies;

    @BeforeEach
    void setUp() {
        verificationStrategy = mock(MailSendStrategy.class);
        welcomeStrategy = mock(MailSendStrategy.class);
        newDeviceLoginStrategy = mock(MailSendStrategy.class);

        given(verificationStrategy.getPurpose()).willReturn(NotificationPurpose.VERIFICATION);
        given(welcomeStrategy.getPurpose()).willReturn(NotificationPurpose.WELCOME);
        given(newDeviceLoginStrategy.getPurpose()).willReturn(NotificationPurpose.NEW_DEVICE_LOGIN);

        // PASSWORD_RESET은 의도적으로 등록하지 않음 → unknownPurpose 시나리오에서 재사용
        senderWithAllStrategies = new EmailNotificationSender(
                List.of(verificationStrategy, welcomeStrategy, newDeviceLoginStrategy));
    }

    private static NotificationRequest buildRequest(NotificationPurpose purpose) {
        return NotificationRequest.of(
                NotificationChannel.EMAIL,
                "test@example.com",
                "[HAN] 테스트",
                "<html>content</html>",
                purpose
        );
    }

    @Test
    @DisplayName("getChannel() → EMAIL을 반환한다")
    void getChannel_returnsEmail() {
        assertThat(senderWithAllStrategies.getChannel()).isEqualTo(NotificationChannel.EMAIL);
    }

    @Nested
    @DisplayName("생성자(constructor)")
    class Constructor {

        @Test
        @DisplayName("등록되지 않은 Purpose가 있어도 예외 없이 생성되고, 런타임에 실패를 위임한다")
        void missingStrategy_doesNotThrowOnConstruction() {
            // given - PASSWORD_RESET strategy가 없는 상태로 생성
            // then - 생성자에서 예외가 나오면 안 됨 (경고 로그만 남기는 설계)
            // 실제 실패는 send() 호출 시점으로 위임됨 → fail-fast 대신 fail-on-use 전략
            new EmailNotificationSender(List.of(verificationStrategy, welcomeStrategy));
        }

        @Test
        @DisplayName("모든 Purpose에 대해 strategy가 등록되면 완전한 sender가 구성된다")
        void allStrategiesRegistered_senderIsComplete() {
            // given - 모든 Purpose에 대응하는 전략 목록 구성
            List<MailSendStrategy> allStrategies = Arrays.stream(NotificationPurpose.values())
                    .map(purpose -> {
                        MailSendStrategy s = mock(MailSendStrategy.class);
                        given(s.getPurpose()).willReturn(purpose);
                        return s;
                    })
                    .collect(Collectors.toList());

            // when & then - 경고 없이 생성되어야 함 (경고 발생 시 런타임 실패 가능성 있음)
            EmailNotificationSender completeSender = new EmailNotificationSender(allStrategies);
            assertThat(completeSender).isNotNull();
        }
    }

    @Nested
    @DisplayName("send()")
    class Send {

        @ParameterizedTest(name = "{0} → 해당 Purpose의 Strategy에만 위임한다")
        @EnumSource(value = NotificationPurpose.class, names = {"VERIFICATION", "WELCOME", "NEW_DEVICE_LOGIN"})
        @DisplayName("등록된 purpose → 해당 Strategy에만 위임하고, 다른 Strategy는 호출되지 않는다")
        void registeredPurpose_delegatesToCorrectStrategyOnly(NotificationPurpose purpose) {
            NotificationRequest request = buildRequest(purpose);

            senderWithAllStrategies.send(request);

            MailSendStrategy expectedStrategy = switch (purpose) {
                case VERIFICATION -> verificationStrategy;
                case WELCOME -> welcomeStrategy;
                case NEW_DEVICE_LOGIN -> newDeviceLoginStrategy;
                default -> throw new IllegalArgumentException("unexpected: " + purpose);
            };

            then(expectedStrategy).should(times(1)).send(request);

            // 나머지 전략은 절대 호출되지 않아야 함
            List.of(verificationStrategy, welcomeStrategy, newDeviceLoginStrategy)
                    .stream()
                    .filter(s -> s != expectedStrategy)
                    .forEach(s -> then(s).should(never()).send(any()));
        }

        @Test
        @DisplayName("Strategy에서 MailException 발생 → 원본 예외를 그대로 호출자에게 전파한다")
        void strategyThrows_propagatesOriginalExceptionToCaller() {
            // given
            NotificationRequest request = buildRequest(NotificationPurpose.VERIFICATION);
            MailSendException originalException = new MailSendException("SMTP Connection refused");
            willThrow(originalException).given(verificationStrategy).send(request);

            // when & then
            // 예외 타입 + 메시지 동시 검증 → 중간 wrapping 여부까지 감지
            assertThatThrownBy(() -> senderWithAllStrategies.send(request))
                    .isInstanceOf(MailSendException.class)
                    .hasMessage("SMTP Connection refused");
        }

        @Test
        @DisplayName("등록되지 않은 purpose → Strategy 조회 실패 → INTERNAL_SERVER_ERROR를 던진다")
        void unknownPurpose_throwsInternalServerError() {
            // given - PASSWORD_RESET strategy가 등록되지 않은 senderWithAllStrategies 사용
            NotificationRequest request = buildRequest(NotificationPurpose.PASSWORD_RESET);

            // when & then
            // CustomException + 정확한 status 코드까지 검증
            assertThatThrownBy(() -> senderWithAllStrategies.send(request))
                    .isInstanceOf(CustomException.class)
                    .extracting("status")
                    .isEqualTo(ResponseStatus.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("등록되지 않은 purpose 발송 시 다른 Strategy는 절대 호출되지 않는다")
        void unknownPurpose_noStrategyIsCalled() {
            // given
            NotificationRequest request = buildRequest(NotificationPurpose.PASSWORD_RESET);

            // when
            try {
                senderWithAllStrategies.send(request);
            } catch (CustomException ignored) {
                // 예외 발생은 이미 위 테스트에서 검증 — 여기서는 side-effect만 검증
            }

            // then - 전략 조회 실패 시 어떤 전략도 호출되어서는 안 됨
            then(verificationStrategy).should(never()).send(any());
            then(welcomeStrategy).should(never()).send(any());
        }
    }

}