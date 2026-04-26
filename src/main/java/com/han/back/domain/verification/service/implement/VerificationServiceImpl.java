package com.han.back.domain.verification.service.implement;

import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.NotificationChannel;
import com.han.back.global.infra.notification.NotificationDispatcher;
import com.han.back.global.infra.notification.NotificationPurpose;
import com.han.back.global.infra.notification.NotificationRequest;
import com.han.back.global.infra.notification.template.MailTemplateUtil;
import com.han.back.global.infra.redis.util.RedisUtil;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.util.MaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    private final RedisUtil redisUtil;
    private final MailTemplateUtil mailTemplateUtil;
    private final NotificationDispatcher notificationDispatcher;
    private final Map<VerificationType, VerificationPolicy> policyMap;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationServiceImpl(RedisUtil redisUtil,
                                   MailTemplateUtil mailTemplateUtil,
                                   NotificationDispatcher notificationDispatcher,
                                   List<VerificationPolicy> policies) {
        this.redisUtil = redisUtil;
        this.mailTemplateUtil = mailTemplateUtil;
        this.notificationDispatcher = notificationDispatcher;
        this.policyMap = policies.stream()
                .flatMap(v -> v.getSupportedTypes().stream()
                        .map(type -> Map.entry(type, v)))
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public VerificationSendResponseDto sendCode(VerificationSendRequestDto request) {
        String target = request.getTarget();
        VerificationType type = request.getType();
        NotificationChannel channel = request.getChannel();

        channel.validateSupported();
        runPolicyCheck(type, target, channel);
        validateCooldown(type, target);

        String code = generateCode();
        storeCodeAndCooldown(type, target, code);

        String content = buildContent(channel, type, code);

        notificationDispatcher.dispatch(NotificationRequest.of(
                channel, target, type.getEmailSubject(), content,
                NotificationPurpose.VERIFICATION,
                "verification:" + type.name() + ":" + MaskingUtil.maskTarget(target),
                "verification:" + type.name() + ":" + target + ":" + code
        ));

        log.info("Verification code dispatched - Type: {} | Channel: {} | Target: {}",
                type, channel, MaskingUtil.maskTarget(target));

        return VerificationSendResponseDto.of(VerificationConst.CODE_TTL, VerificationConst.COOLDOWN_TTL);
    }

    @Override
    public void confirmCode(VerificationConfirmRequestDto request) {
        String target = request.getTarget();
        String code = request.getCode();
        VerificationType type = request.getType();

        String codeKey = VerificationConst.codeKey(type, target);
        String storedCode = redisUtil.getData(codeKey)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.VERIFICATION_EXPIRED));

        if (!storedCode.equals(code)) {
            throw new CustomException(BaseResponseStatus.VERIFICATION_FAIL);
        }

        redisUtil.deleteData(codeKey);
        redisUtil.setDataExpire(
                VerificationConst.confirmedKey(type, target),
                "CONFIRMED",
                VerificationConst.CONFIRMED_TTL
        );

        log.info("Verification code confirmed - Type: {} | Target: {}",
                type, MaskingUtil.maskTarget(target));
    }

    @Override
    public void validateConfirmed(String target, VerificationType type) {
        if (!redisUtil.hasKey(VerificationConst.confirmedKey(type, target))) {
            throw new CustomException(BaseResponseStatus.VERIFICATION_NOT_COMPLETED);
        }
    }

    @Override
    public void consumeConfirmation(String target, VerificationType type) {
        redisUtil.deleteData(VerificationConst.confirmedKey(type, target));
    }

    private void runPolicyCheck(VerificationType type, String target, NotificationChannel channel) {
        VerificationPolicy policy = policyMap.get(type);
        if (policy != null) {
            policy.check(target, channel);
        }
    }

    private void validateCooldown(VerificationType type, String target) {
        if (redisUtil.hasKey(VerificationConst.cooldownKey(type, target))) {
            throw new CustomException(BaseResponseStatus.COOLDOWN_ACTIVE);
        }
    }

    private void storeCodeAndCooldown(VerificationType type, String target, String code) {
        redisUtil.setDataExpire(VerificationConst.codeKey(type, target), code, VerificationConst.CODE_TTL);
        redisUtil.setDataExpire(VerificationConst.cooldownKey(type, target), "ACTIVE", VerificationConst.COOLDOWN_TTL);
    }

    private String buildContent(NotificationChannel channel, VerificationType type, String code) {
        if (channel == NotificationChannel.EMAIL) {
            return mailTemplateUtil.buildVerificationEmail(
                    code, type.getDescription(), VerificationConst.CODE_TTL_MINUTES);
        }
        return String.format("[HAN] %s code: %s (valid for %d min)",
                type.getDescription(), code, VerificationConst.CODE_TTL_MINUTES);
    }

    private String generateCode() {
        return String.format("%0" + VerificationConst.CODE_LENGTH + "d",
                secureRandom.nextInt(VerificationConst.CODE_BOUND));
    }

}