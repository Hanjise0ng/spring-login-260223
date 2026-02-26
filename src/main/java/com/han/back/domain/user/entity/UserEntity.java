package com.han.back.domain.user.entity;

import com.han.back.global.entity.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.List;


@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "user")
public class UserEntity extends BaseTime {

    @Column(unique = true, nullable = false)
    private String userId;

    private String password;

    private String email;

    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String type;

    public void updateUsername(String username) {
        this.username = username;
    }

    public List<GrantedAuthority> getAuthorities() {
        return Collections.singletonList(this.role);
    }

    public boolean hasRole(Role role) {
        return this.role == role;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

}