package com.han.back.domain.user.entity;

import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.entity.BaseTime;
import com.han.back.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_login_id", columnNames = {"login_id"}),
                @UniqueConstraint(name = "uk_users_email",    columnNames = {"email"})
        },
        indexes = {
                @Index(name = "idx_users_login_id", columnList = "login_id")
        }
)
public class UserEntity extends BaseTime {

    @Column(name = "login_id", unique = true, nullable = false, length = 30)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider authProvider;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void changePassword(String encodedPassword) {
        if (this.authProvider.isSocial()) {
            throw new CustomException(BaseResponseStatus.SOCIAL_ONLY_ACCOUNT);
        }
        this.password = encodedPassword;
    }

    public boolean isLocalUser() {
        return this.authProvider == AuthProvider.LOCAL;
    }

    public boolean isSocialUser() {
        return this.authProvider.isSocial();
    }

}