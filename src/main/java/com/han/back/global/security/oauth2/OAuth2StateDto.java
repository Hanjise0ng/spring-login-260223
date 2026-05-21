package com.han.back.global.security.oauth2;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;
import java.util.Set;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
class OAuth2StateDto {

    private String authorizationUri;
    private String clientId;
    private String redirectUri;
    private Set<String> scopes;
    private String state;
    private Map<String, Object> additionalParameters;
    private Map<String, Object> attributes;
    private String authorizationRequestUri;

    static OAuth2StateDto from(OAuth2AuthorizationRequest request) {
        return new OAuth2StateDto(
                request.getAuthorizationUri(),
                request.getClientId(),
                request.getRedirectUri(),
                request.getScopes(),
                request.getState(),
                request.getAdditionalParameters(),
                request.getAttributes(),
                request.getAuthorizationRequestUri());
    }

    OAuth2AuthorizationRequest toAuthorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri(authorizationUri)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .scopes(scopes)
                .state(state)
                .additionalParameters(
                        additionalParameters != null ? additionalParameters : Map.of())
                .attributes(
                        attributes != null ? attributes : Map.of())
                .authorizationRequestUri(authorizationRequestUri)
                .build();
    }

}