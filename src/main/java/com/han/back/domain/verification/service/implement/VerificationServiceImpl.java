package com.han.back.domain.verification.service.implement;

import com.han.back.domain.verification.dto.request.VerificationConfirmRequestDto;
import com.han.back.domain.verification.dto.request.VerificationSendRequestDto;
import com.han.back.domain.verification.dto.response.VerificationSendResponseDto;
import com.han.back.domain.verification.entity.VerificationConst;
import com.han.back.domain.verification.entity.VerificationType;
import com.han.back.domain.verification.service.VerificationPolicy;
import com.han.back.domain.verification.service.VerificationService;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import com.han.back.global.notification.NotificationChannel;
import com.han.back.global.notification.NotificationSender;
import com.han.back.global.notification.template.MailTemplateUtil;
import com.han.back.global.security.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    private final RedisUtil redisUtil;
    private final MailTemplateUtil mailTemplateUtil;
    private final Map<NotificationChannel, NotificationSender> senderMap;
    private final Map<VerificationType, VerificationPolicy> policyMap;
    private final SecureRandom secureRandom = new SecureRandom();

    public VerificationServiceImpl(RedisUtil redisUtil, MailTemplateUtil mailTemplateUtil,
                                   List<NotificationSender> senders, List<VerificationPolicy> policies) {
        this.redisUtil = redisUtil;
        this.mailTemplateUtil = mailTemplateUtil;
        this.senderMap = senders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationSender::getChannel,
                        Function.identity()
                ));
        this.policyMap = policies.stream()
                .flatMap(v -> v.getSupportedTypes().stream()
                        .map(type -> Map.entry(type, v)))
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    @Override
    public VerificationSendResponseDto sendCode(VerificationSendRequestDto request) {
        String target = request.getTarget();
        VerificationType type = request.getType();
        NotificationChannel channel = request.getChannel();

        channel.validateSupported();

        NotificationSender sender = resolveSender(channel);

        runPolicyCheck(type, target);
        validateCooldown(type, target);

        String code = generateCode();
        storeCodeAndCooldown(type, target, code);

        String content = buildContent(channel, type, code);
        sender.send(target, type.getEmailSubject(), content);

        log.info("Verification code sent - Type: {} | Channel: {} | Target: {}",
                type, channel, maskTarget(target));

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

        log.info("Verification code confirmed - Type: {} | Target: {}", type, maskTarget(target));
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

    private void runPolicyCheck(VerificationType type, String target) {
        VerificationPolicy policy = policyMap.get(type);
        if (policy != null) {
            policy.check(target);
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

    private NotificationSender resolveSender(NotificationChannel channel) {
        NotificationSender sender = senderMap.get(channel);
        if (sender == null) {
            log.error("NotificationSender implementation not found for channel: {}", channel);
            throw new CustomException(BaseResponseStatus.INTERNAL_SERVER_ERROR);
        }
        return sender;
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

    private String maskTarget(String target) {
        if (target.contains("@")) {
            int atIndex = target.indexOf("@");
            if (atIndex <= 2) return "***" + target.substring(atIndex);
            return target.substring(0, 2) + "***" + target.substring(atIndex);
        }
        if (target.length() > 4) {
            return target.substring(0, 3) + "****" + target.substring(target.length() - 4);
        }
        return "****";
    }

}