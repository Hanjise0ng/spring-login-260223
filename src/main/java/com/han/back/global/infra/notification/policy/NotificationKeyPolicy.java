package com.han.back.global.infra.notification.policy;

import org.springframework.stereotype.Component;

@Component
public class NotificationKeyPolicy {

    public String welcome(Long userId) {
        return "welcome:user:" + userId;
    }

    public String verification(String typeName, String target) {
        return "verification:" + typeName + ":" + target;
    }

    public String passwordReset(String email) {
        return "password-reset:" + email;
    }

}