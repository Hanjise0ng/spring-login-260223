package com.han.back.domain.user.mapper;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity ofSignupDTO(SignUpRequestDto dto) {
        return UserEntity.builder()
                .userId(dto.getUserId())
                .password(dto.getPassword())
                .email(dto.getEmail())
                .username(dto.getUsername())
                .role(Role.USER)
                .type("app")
                .build();
    }

}
