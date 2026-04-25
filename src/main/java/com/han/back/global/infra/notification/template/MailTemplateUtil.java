package com.han.back.global.infra.notification.template;

import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MailTemplateUtil {

    private static final String LAYOUT_TEMPLATE = "templates/mail/layout.html";
    private static final String VERIFICATION_TEMPLATE = "templates/mail/verification-code.html";
    private static final String WELCOME_TEMPLATE = "templates/mail/welcome.html";

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final ConcurrentMap<String, String> templateCache = new ConcurrentHashMap<>();

    @Value("${app.front-base-url:http://localhost:3000}")
    private String frontBaseUrl;

    public String buildVerificationEmail(String code, String typeDescription, long expiresMinutes) {
        String body = render(VERIFICATION_TEMPLATE, Map.of(
                "CODE", escapeHtml(code),
                "TYPE_DESCRIPTION", escapeHtml(typeDescription),
                "EXPIRES_MINUTES", String.valueOf(expiresMinutes)
        ));
        return wrapWithLayout(body, "이메일 인증 안내");
    }

    public String buildWelcomeEmail(String nickname, LocalDateTime signedUpAt) {
        String body = render(WELCOME_TEMPLATE, Map.of(
                "NICKNAME", escapeHtml(nickname),
                "SIGNED_UP_AT", signedUpAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "MY_PAGE_URL", escapeHtml(frontBaseUrl + "/mypage")
        ));
        return wrapWithLayout(body, "가입을 환영합니다");
    }

    private String wrapWithLayout(String bodyContent, String title) {
        return render(LAYOUT_TEMPLATE, Map.of(
                "BODY", bodyContent,
                "TITLE", escapeHtml(title)
        ));
    }

    private String render(String templatePath, Map<String, String> values) {
        String template = getTemplate(templatePath);
        String result = replacePlaceholders(template, values);
        validateNoUnresolvedPlaceholders(result, templatePath);
        return result;
    }

    private String replacePlaceholders(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    private void validateNoUnresolvedPlaceholders(String result, String templatePath) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(result);
        if (matcher.find()) {
            log.warn("Unresolved placeholder in template - path: {} | placeholder: {{{}}}",
                    templatePath, matcher.group(1));
        }
    }

    private String getTemplate(String path) {
        return templateCache.computeIfAbsent(path, this::loadTemplate);
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

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}