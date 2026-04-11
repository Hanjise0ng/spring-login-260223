package com.han.back.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.NoArgsConstructor;

@JsonInclude(Include.NON_NULL)
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Empty {
    private static final Empty INSTANCE = new Empty();
    public static Empty getInstance() {
        return INSTANCE;
    }
}