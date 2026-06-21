package com.han.back.domain.auth.oauth2.service;

import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.auth.oauth2.adapter.OAuth2UserInfo;
import com.han.back.domain.auth.oauth2.exception.SocialResponseStatus;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLinkService {

    private final SocialLinkStateCache socialLinkStateCache;
    private final CredentialLinkService credentialLinkService;

    public void link(String state, OAuth2UserInfo userInfo) {
        Long userId = socialLinkStateCache.consume(state)
                .orElseThrow(() -> new CustomException(SocialResponseStatus.SOCIAL_LINK_TOKEN_INVALID));

        AuthProvider provider = userInfo.getProvider();
        credentialLinkService.linkSocialCredential(userId, provider, userInfo.getProviderId());

        log.info("Social Linked - UserPK: {} | Provider: {}", userId, provider);
    }

}