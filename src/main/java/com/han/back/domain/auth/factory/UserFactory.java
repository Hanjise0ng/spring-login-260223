package com.han.back.domain.auth.factory;

import com.han.back.domain.auth.credential.entity.CredentialEntity;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.global.util.UuidUtil;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    public UserEntity createLocalUser(SignUpRequestDto dto, String tag) {
        return UserEntity.builder()
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .tag(tag)
                .role(Role.USER)
                .build();
    }

    public CredentialEntity createLocalCredential(Long userId, String identifier, String encodedPassword) {
        return CredentialEntity.builder()
                .userId(userId)
                .provider(AuthProvider.LOCAL)
                .identifier(identifier)
                .password(encodedPassword)
                .build();
    }

    public UserEntity createSocialUser(String nickname, String email, String tag) {
        return UserEntity.builder()
                .publicId(UuidUtil.generateString())
                .nickname(nickname)
                .email(email)
                .tag(tag)
                .role(Role.USER)
                .build();
    }

    public CredentialEntity createSocialCredential(Long userId, AuthProvider provider, String providerId) {
        return CredentialEntity.builder()
                .userId(userId)
                .provider(provider)
                .identifier(providerId)
                .build();
    }

}