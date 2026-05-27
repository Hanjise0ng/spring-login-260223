package com.han.back.global.infra.notification.template;

import com.han.back.domain.device.entity.DeviceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MailTemplateUtil")
class MailTemplateUtilTest {

    private MailTemplateUtil mailTemplateUtil;

    @BeforeEach
    void setUp() {
        mailTemplateUtil = new MailTemplateUtil();
        ReflectionTestUtils.setField(mailTemplateUtil, "frontBaseUrl", "https://test.han.com");
    }

    @Test
    @DisplayName("인증 코드 메일 — 코드와 만료 시간이 치환된다")
    void verificationEmail_replacesPlaceholders() {
        String result = mailTemplateUtil.buildVerificationEmail("123456", "회원가입 인증", 5);

        assertThat(result).contains("123456");
        assertThat(result).contains("5분");
        assertThat(result).contains("HAN");
    }

    @Test
    @DisplayName("환영 메일 — 닉네임, 가입일, 마이페이지 URL 이 치환된다")
    void welcomeEmail_replacesPlaceholders() {
        String result = mailTemplateUtil.buildWelcomeEmail(
                "홍길동", LocalDateTime.of(2026, 4, 26, 0, 0));

        assertThat(result).contains("홍길동");
        assertThat(result).contains("2026-04-26");
        assertThat(result).contains("https://test.han.com/mypage");
        assertThat(result).contains("HAN");
    }

    @Test
    @DisplayName("XSS 방어 — 닉네임에 HTML 태그가 있으면 escape 된다")
    void welcomeEmail_escapesHtmlInNickname() {
        String result = mailTemplateUtil.buildWelcomeEmail(
                "<script>alert(1)</script>", LocalDateTime.now());

        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("레이아웃 적용 — 헤더와 푸터가 포함된다")
    void allEmails_includeLayoutHeaderAndFooter() {
        String result = mailTemplateUtil.buildVerificationEmail("000000", "테스트", 3);

        assertThat(result).containsIgnoringCase("<!DOCTYPE html>");
        assertThat(result).contains("발신 전용");
        assertThat(result).contains("© HAN Service");
    }

    @Test
    @DisplayName("신규 기기 로그인 메일 — 닉네임, 기기 정보, 보안 URL이 치환된다")
    void newDeviceLoginEmail_replacesPlaceholders() {
        String result = mailTemplateUtil.buildNewDeviceLoginEmail(
                "홍길동",
                DeviceType.WEB_DESKTOP,
                "Windows 10",
                "192.168.0.1",
                LocalDateTime.of(2026, 5, 1, 14, 30)
        );

        assertThat(result).contains("홍길동");
        assertThat(result).contains("WEB_DESKTOP");
        assertThat(result).contains("Windows 10");
        assertThat(result).contains("192.168.0.1");
        assertThat(result).contains("2026-05-01 14:30");
        assertThat(result).contains("https://test.han.com/settings/security");
        assertThat(result).contains("HAN");
    }

    @Test
    @DisplayName("신규 기기 로그인 메일 — 닉네임에 HTML 태그가 있으면 escape 된다")
    void newDeviceLoginEmail_escapesHtmlInNickname() {
        String result = mailTemplateUtil.buildNewDeviceLoginEmail(
                "<script>alert(1)</script>",
                DeviceType.WEB_MOBILE,
                "Android 14",
                "10.0.0.1",
                LocalDateTime.now()
        );

        assertThat(result).doesNotContain("<script>");
        assertThat(result).contains("&lt;script&gt;");
    }

}