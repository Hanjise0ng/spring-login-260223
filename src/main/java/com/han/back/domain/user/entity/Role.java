package com.han.back.domain.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@Getter
@RequiredArgsConstructor
public enum Role implements GrantedAuthority {
    GUEST("GUEST"),
    USER("USER"),
    ADMIN("ADMIN");

    private final String description;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }

}