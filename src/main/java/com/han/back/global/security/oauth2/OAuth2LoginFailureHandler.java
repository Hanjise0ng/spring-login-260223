package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Value("${app.front-base-url}")
    private String frontBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 Login Failed - Error: {}", exception.getMessage());

        String redirectUrl = UriComponentsBuilder
                .fromUriString(frontBaseUrl)
                .path(OAuth2Const.FRONT_LOGIN_ERROR_PATH)
                .queryParam("error",
                        OAuth2Const.ERROR_SOCIAL_LOGIN_FAILED)
                .build().encode().toUriString();

        response.sendRedirect(redirectUrl);
    }

}