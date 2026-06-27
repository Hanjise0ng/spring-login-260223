package com.han.back.domain.auth.credential.entity;

import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.entity.BaseTime;
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
        name = "credentials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_credentials_provider_identifier",
                        columnNames = {"provider", "identifier"}),
                @UniqueConstraint(
                        name = "uk_credentials_user_provider",
                        columnNames = {"user_id", "provider"})
        },
        indexes = {
                @Index(name = "idx_credentials_user_id", columnList = "user_id")
        }
)
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "credential_id"))
})
public class CredentialEntity extends BaseTime {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "identifier", nullable = false, length = 100)
    private String identifier;

    @Column(name = "password", length = 255)
    private String password;

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public boolean isLocal() {
        return this.provider == AuthProvider.LOCAL;
    }

}