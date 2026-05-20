package com.han.back.domain.user.service;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class TagGenerator {

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate(String nickname) {
        for (int i = 0; i < OAuth2Const.TAG_GENERATION_RETRY; i++) {
            String tag = randomHexTag();
            if (!userRepository.existsByNicknameAndTag(nickname, tag)) {
                return tag;
            }
        }
        throw new CustomException(BaseResponseStatus.TAG_GENERATION_FAILED);
    }

    private String randomHexTag() {
        int value = secureRandom.nextInt(0x10000);
        return String.format("%04X", value);
    }

}