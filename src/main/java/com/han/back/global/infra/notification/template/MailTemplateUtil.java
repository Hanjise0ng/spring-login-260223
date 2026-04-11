package com.han.back.global.infra.notification.template;

import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class MailTemplateUtil {

    private static final String VERIFICATION_TEMPLATE_PATH = "templates/mail/verification-code.html";

    private final String verificationTemplate;

    public MailTemplateUtil() {
        this.verificationTemplate = loadTemplate(VERIFICATION_TEMPLATE_PATH);
    }

    public String buildVerificationEmail(String code, String typeDescription, long expiresMinutes) {
        return replacePlaceholders(verificationTemplate, Map.of(
                "CODE", code,
                "TYPE_DESCRIPTION", typeDescription,
                "EXPIRES_MINUTES", String.valueOf(expiresMinutes)
        ));
    }

    private String replacePlaceholders(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private String loadTemplate(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            log.info("Mail template loaded successfully: path={}", path);
            return template;
        } catch (IOException e) {
            log.error("Mail template load failed: path={}", path, e);
            throw new CustomException(BaseResponseStatus.MAIL_TEMPLATE_LOAD_FAIL);
        }
    }

}