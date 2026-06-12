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
    private final String email;
    private final String nickname;
    private final boolean deleted;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserDetails(Long id, String password, Role role, String sessionId, String email, String nickname, boolean deleted) {
        this.id = id;
        this.password = password;
        this.role = role;
        this.sessionId = sessionId;
        this.email = email;
        this.nickname = nickname;
        this.deleted = deleted;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority(role.getAuthority()));
    }

    // 로컬 로그인 — 비번 대조와 탈퇴 분기 필요
    public static CustomUserDetails forLocalLogin(Long id, String password, Role role, String email, String nickname, boolean deleted) {
        return new CustomUserDetails(id, password, role, null, email, nickname, deleted);
    }

    // 소셜 로그인 — 비번 없음, 탈퇴 분기 대상 아님
    public static CustomUserDetails forSocialLogin(Long id, Role role, String email, String nickname) {
        return new CustomUserDetails(id, null, role, null, email, nickname, false);
    }

    // JWT 토큰에서 복원한 인증 주체 — AT/RT 인증 토큰 기반 복원용 (id·role·sessionId만 보유)
    public static CustomUserDetails fromToken(Long id, Role role, String sessionId) {
        return new CustomUserDetails(id, null, role, sessionId, null, null, false);
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

    public String getEmail() {
        return this.email;
    }

    public String getNickname() {
        return this.nickname;
    }

    public boolean isDeleted() {
        return this.deleted;
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