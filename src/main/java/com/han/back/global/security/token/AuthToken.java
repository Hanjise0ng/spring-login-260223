package com.han.back.global.security.token;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthToken {

    private String accessToken;

    private String refreshToken;

    public static AuthToken of(String accessToken, String refreshToken) {
        return AuthToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public boolean hasAccessToken() {
        return StringUtils.hasText(this.accessToken);
    }

    public boolean hasRefreshToken() {
        return StringUtils.hasText(this.refreshToken);
    }

    public boolean isEmpty() {
        return !hasAccessToken() && !hasRefreshToken();
    }

}