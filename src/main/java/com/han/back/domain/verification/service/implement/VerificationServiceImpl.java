package com.han.back.domain.verification.service.implement;

import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.exception.VerificationResponseStatus;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.exception.CustomException;
import com.han.back.global.infra.notification.dispatcher.NotificationDispatcher;
import com.han.back.global.infra.notification.model.*;
import com.han.back.global.infra.notification.policy.NotificationKeyPolicy;
import com.han.back.global.infra.notification.template.MailTemplateUtil;
import com.han.back.global.infra.redis.util.RateLimitUtil;
import com.han.back.global.infra.redis.util.RedisUtil;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.trace.TraceContext;
import com.han.back.global.util.MaskingUtil;
import com.han.back.global.util.RateLimitConst;
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
    private final RateLimitUtil rateLimitUtil;
    private final MailTemplateUtil mailTemplateUtil;
    private final NotificationDispatcher notificationDispatcher;
    private final NotificationKeyPolicy keyPolicy;
    private final Map<VerificationType, VerificationPolicy> policyMap;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationServiceImpl(RedisUtil redisUtil,
                                   RateLimitUtil rateLimitUtil,
                                   MailTemplateUtil mailTemplateUtil,
                                   NotificationDispatcher notificationDispatcher,
                                   NotificationKeyPolicy keyPolicy,
                                   List<VerificationPolicy> policies) {
        this.redisUtil = redisUtil;
        this.rateLimitUtil = rateLimitUtil;
        this.mailTemplateUtil = mailTemplateUtil;
        this.notificationDispatcher = notificationDispatcher;
        this.keyPolicy = keyPolicy;
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
        validateHourlyLimit(type, target);

        String code = generateCode();
        storeCodeAndCooldown(type, target, code);

        String content = buildContent(channel, type, code);

        notificationDispatcher.dispatch(NotificationCommand.of(
                NotificationRequest.of(
                        channel, target, type.getEmailSubject(), content,
                        NotificationPurpose.VERIFICATION
                ),
                NotificationMetadata.of(
                        TraceContext.getTraceId(),
                        keyPolicy.verification(type.name(), target)
                )
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
                .orElseThrow(() -> new CustomException(VerificationResponseStatus.VERIFY_CODE_EXPIRED));

        if (!storedCode.equals(code)) {
            handleFailedVerification(type, target, codeKey);
        }

        String failKey = buildFailKey(type, target);
        rateLimitUtil.reset(failKey);

        redisUtil.deleteData(codeKey);
        redisUtil.setDataExpire(
                VerificationConst.confirmedKey(type, target),
                "CONFIRMED",
                VerificationConst.CONFIRMED_TTL);

        log.info("Verification code confirmed - Type: {} | Target: {}",
                type, MaskingUtil.maskTarget(target));
    }

    @Override
    public void validateConfirmed(String target, VerificationType type) {
        if (!redisUtil.hasKey(VerificationConst.confirmedKey(type, target))) {
            throw new CustomException(VerificationResponseStatus.VERIFY_NOT_COMPLETED);
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
            throw new CustomException(VerificationResponseStatus.VERIFY_COOLDOWN);
        }
    }

    private void validateHourlyLimit(VerificationType type, String target) {
        String keyPrefix = RateLimitConst.VERIFY_SEND_PREFIX + type.name() + ":" + target;
        long count = rateLimitUtil.incrementHourly(keyPrefix);

        if (count > RateLimitConst.VERIFY_SEND_HOURLY_MAX) {
            log.warn("Hourly send limit exceeded - Type: {} | Target: {}",
                    type, MaskingUtil.maskTarget(target));
            throw new CustomException(ResponseStatus.RATE_LIMIT_EXCEEDED);
        }
    }

    private void storeCodeAndCooldown(VerificationType type, String target, String code) {
        redisUtil.setDataExpire(VerificationConst.codeKey(type, target),
                code, VerificationConst.CODE_TTL);
        redisUtil.setDataExpire(VerificationConst.cooldownKey(type, target),
                "ACTIVE", VerificationConst.COOLDOWN_TTL);
    }

    private String buildContent(NotificationChannel channel, VerificationType type, String code) {
        if (channel == NotificationChannel.EMAIL) {
            return mailTemplateUtil.buildVerificationEmail(
                    code, type.getDescription(),
                    VerificationConst.CODE_TTL.toMinutes());
        }
        return String.format("[HAN] %s code: %s (valid for %d min)",
                type.getDescription(), code,
                VerificationConst.CODE_TTL.toMinutes());
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(VerificationConst.CODE_LENGTH);
        for (int i = 0; i < VerificationConst.CODE_LENGTH; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private void handleFailedVerification(VerificationType type, String target, String codeKey) {
        String failKey = buildFailKey(type, target);
        long failCount = rateLimitUtil.increment(failKey, VerificationConst.CODE_TTL);

        if (failCount >= RateLimitConst.VERIFY_FAIL_MAX) {
            redisUtil.deleteData(codeKey);
            rateLimitUtil.reset(failKey);
            log.warn("Verification code invalidated by max failures - Type: {} | Target: {}",
                    type, MaskingUtil.maskTarget(target));
            throw new CustomException(VerificationResponseStatus.VERIFY_CODE_EXPIRED);
        }

        throw new CustomException(VerificationResponseStatus.VERIFY_CODE_MISMATCH);
    }

    private String buildFailKey(VerificationType type, String target) {
        return RateLimitConst.VERIFY_FAIL_PREFIX + type.name() + ":" + target;
    }

}