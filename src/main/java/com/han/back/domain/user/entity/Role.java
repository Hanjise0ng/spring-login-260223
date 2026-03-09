package com.han.back.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum Role implements GrantedAuthority {
    GUEST("GUEST"),
    USER("USER"),
    ADMIN("ADMIN");

    private final String value;

    private static final Map<String, Role> AUTHORITY_MAP =
            Collections.unmodifiableMap(
                    Stream.of(values())
                            .collect(Collectors.toMap(Role::getAuthority, Function.identity()))
            );

    @Override
    public String getAuthority() {
        return "ROLE_" + this.value;
    }

    public static Role fromAuthority(String authority) {
        return AUTHORITY_MAP.get(authority);
    }

}