package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserResolver;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.auth.oauth2.repository.SocialAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final OAuth2UserResolver oauth2UserResolver;
    private final SocialAccountRepository socialAccountRepository;
    private final DefaultOAuth2UserService defaultOAuth2UserService;

    @Override
    @Transactional(readOnly = true)
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = defaultOAuth2UserService.loadUser(request);
        OAuth2UserInfo userInfo = resolveUserInfo(request, oAuth2User);
        return buildOAuth2User(oAuth2User, userInfo, request);
    }

    private OAuth2UserInfo resolveUserInfo(OAuth2UserRequest request, OAuth2User oAuth2User) {
        String registrationId = request.getClientRegistration().getRegistrationId();
        return oauth2UserResolver.resolve(registrationId, oAuth2User.getAttributes());
    }

    private OAuth2User buildOAuth2User(OAuth2User oAuth2User,
                                       OAuth2UserInfo userInfo,
                                       OAuth2UserRequest request) {
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        attributes.put(OAuth2Const.ATTR_USER_INFO, userInfo);
        enrichWithExistingUser(attributes, userInfo);

        String userNameAttributeName = request.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new DefaultOAuth2User(oAuth2User.getAuthorities(), attributes, userNameAttributeName);
    }

    private void enrichWithExistingUser(Map<String, Object> attributes, OAuth2UserInfo userInfo) {
        socialAccountRepository
                .findByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())
                .ifPresent(account ->
                        attributes.put(OAuth2Const.ATTR_EXISTING_USER, account.getUserId()));
    }


}