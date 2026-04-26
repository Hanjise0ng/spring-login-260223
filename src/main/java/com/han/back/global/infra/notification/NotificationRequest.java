package com.han.back.global.infra.notification;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationRequest {

    private final NotificationChannel channel;     // 어떤 채널로 (EMAIL, SMS...)
    private final String target;                   // 수신 대상 (이메일 주소, 전화번호)
    private final String subject;                  // 제목
    private final String content;                  // 본문 (HTML)
    private final NotificationPurpose purpose;     // 용도 (재시도 정책 분기 키)
    private final String traceKey;                 // 로그 추적용 — 요청마다 고유
    private final String dedupeKey;                // 중복 차단용 — 같은 의도면 같은 값
    private final LocalDateTime createdAt;

    public static NotificationRequest of(NotificationChannel channel,
                                         String target,
                                         String subject,
                                         String content,
                                         NotificationPurpose purpose,
                                         String traceKey,
                                         String dedupeKey) {
        return new NotificationRequest(
                channel, target, subject, content, purpose,
                traceKey, dedupeKey, LocalDateTime.now()
        );
    }

}