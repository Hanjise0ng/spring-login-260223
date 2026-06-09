package com.han.back.domain.user.service;

import com.han.back.domain.auth.oauth2.entity.OAuth2Const;
import com.han.back.domain.user.exception.AccountResponseStatus;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class TagGenerator {

    private final UserRepository userRepository;
    private final SecureRandom secureRandom;

    public String generate(String nickname) {
        for (int i = 0; i < OAuth2Const.TAG_GENERATION_RETRY; i++) {
            String tag = randomHexTag();
            if (!userRepository.existsByNicknameAndTag(nickname, tag)) {
                return tag;
            }
        }
        throw new CustomException(AccountResponseStatus.ACCOUNT_TAG_GENERATION_FAIL);
    }

    private String randomHexTag() {
        int value = secureRandom.nextInt(0x10000);
        return String.format("%04X", value);
    }

}