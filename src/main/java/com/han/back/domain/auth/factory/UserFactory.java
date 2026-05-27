package com.han.back.domain.auth.factory;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.domain.user.entity.Role;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.global.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFactory {

    public UserEntity createFromSignUpRequest(SignUpRequestDto dto, String password, String tag) {
        return UserEntity.builder()
                .loginId(dto.getLoginId())
                .password(password)
                .email(dto.getEmail())
                .nickname(dto.getNickname())
                .tag(tag)
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    public UserEntity createSocialUser(String nickname, String email, String password, AuthProvider provider, String tag) {
        String publicId = UuidUtil.generateString();
        String dummyLoginId = String.format(
                OAuth2Const.DUMMY_LOGIN_ID_FORMAT,
                provider.getValue(),
                publicId.substring(0, 8).toUpperCase());

        return UserEntity.builder()
                .publicId(publicId)
                .loginId(dummyLoginId)
                .password(password)
                .nickname(nickname)
                .email(email)
                .tag(tag)
                .role(Role.USER)
                .authProvider(provider)
                .build();
    }

}