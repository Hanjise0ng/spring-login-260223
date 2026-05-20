package com.han.back.domain.auth.oauth2.entity;

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
        name = "social_accounts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_social_provider_provider_id",
                        columnNames = {"provider", "provider_id"}),
                @UniqueConstraint(
                        name = "uk_social_user_provider",
                        columnNames = {"user_id", "provider"})
        },
        indexes = {
                @Index(
                        name = "idx_social_provider_provider_id",
                        columnList = "provider, provider_id")
        }
)
public class SocialAccountEntity extends BaseTime {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(name = "provider_email", length = 100)
    private String providerEmail;

    public void updateProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

}