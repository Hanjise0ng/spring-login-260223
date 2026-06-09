package com.han.back.domain.auth.oauth2.adapter;

import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.exception.CustomException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OAuth2UserResolver {

    private final Map<AuthProvider, OAuth2UserAdapter> adapterMap;

    public OAuth2UserResolver(List<OAuth2UserAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(
                        OAuth2UserAdapter::getSupportedProvider,
                        Function.identity()));
    }

    public OAuth2UserInfo resolve(String registrationId, Map<String, Object> attributes) {
        AuthProvider provider = AuthProvider.fromRegistrationId(registrationId);
        OAuth2UserAdapter adapter = adapterMap.get(provider);

        if (adapter == null) {
            throw new CustomException(SocialResponseStatus.SOCIAL_PROVIDER_UNSUPPORTED);
        }

        return adapter.convert(attributes);
    }

}