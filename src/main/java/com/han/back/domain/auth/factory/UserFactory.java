package com.han.back.domain.auth.factory;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    public UserEntity createFromSignUpRequest(SignUpRequestDto dto, String encodedPassword) {
        return UserEntity.builder()
                .loginId(dto.getLoginId())
                .password(encodedPassword)
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

}