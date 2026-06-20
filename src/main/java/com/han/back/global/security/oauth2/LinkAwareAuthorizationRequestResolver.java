package com.han.back.global.security.oauth2;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.global.security.token.util.SocialLinkTokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class LinkAwareAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final SocialLinkTokenUtil socialLinkTokenUtil;
    private final SocialLinkContext socialLinkContext;

    public LinkAwareAuthorizationRequestResolver(ClientRegistrationRepository repository,
                                                 SocialLinkTokenUtil socialLinkTokenUtil,
                                                 SocialLinkContext socialLinkContext) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(repository, AUTHORIZATION_BASE_URI);
        this.socialLinkTokenUtil = socialLinkTokenUtil;
        this.socialLinkContext = socialLinkContext;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return bindLinkContext(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return bindLinkContext(request, delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest bindLinkContext(HttpServletRequest request, OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null) {
            return null;
        }

        String linkToken = request.getParameter(OAuth2Const.PARAM_LINK_TOKEN);
        if (!StringUtils.hasText(linkToken)) {
            return authRequest;
        }

        Long userId = socialLinkTokenUtil.validate(linkToken);
        socialLinkContext.save(authRequest.getState(), userId);

        return authRequest;
    }

}