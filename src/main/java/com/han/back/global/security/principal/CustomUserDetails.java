package com.han.back.global.security.principal;

import com.han.back.domain.user.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String password;
    private final Role role;
    private final String sessionId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long id, String password, Role role) {
        this(id, password, role, null);
    }

    public CustomUserDetails(Long id, Role role, String sessionId) {
        this(id, null, role, sessionId);
    }

    private CustomUserDetails(Long id, String password, Role role, String sessionId) {
        this.id = id;
        this.password = password;
        this.role = role;
        this.sessionId = sessionId;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()));
    }

    public Long getId() {
        return this.id;
    }

    public Role getRole() {
        return this.role;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    @Override
    public String getUsername() {
        return String.valueOf(this.id);
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

}