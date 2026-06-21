package com.han.back.domain.auth.oauth2.service;

import java.util.Optional;

public interface SocialLinkStateCache {

    void save(String state, Long userId);

    Optional<Long> consume(String state);

}