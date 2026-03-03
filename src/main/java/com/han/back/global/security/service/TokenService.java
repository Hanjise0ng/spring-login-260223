package com.han.back.global.security.service;

public interface TokenService {

    void saveRefreshToken(Long userId, String rt);

    String getRefreshToken(Long userId);

    void deleteRefreshToken(Long userId);

    void addToBlacklist(String at);

    boolean isBlacklisted(String at);

    void invalidatePreviousTokens(String oldAccessToken, String oldRefreshToken);

}