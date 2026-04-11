package com.han.back.fixture;

import com.han.back.global.security.token.AuthToken;

public final class TokenFixture {

    private TokenFixture() {}

    public static final String FAKE_AT = "fake.access.token";
    public static final String FAKE_RT = "fake.refresh.token";
    public static final String NEW_FAKE_AT = "new.fake.access.token";
    public static final String NEW_FAKE_RT = "new.fake.refresh.token";

    public static AuthToken tokenPair() {
        return AuthToken.of(FAKE_AT, FAKE_RT);
    }

    public static AuthToken newTokenPair() {
        return AuthToken.of(NEW_FAKE_AT, NEW_FAKE_RT);
    }

}