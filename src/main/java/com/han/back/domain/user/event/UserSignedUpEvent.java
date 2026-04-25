package com.han.back.domain.user.event;

import com.han.back.domain.user.entity.UserEntity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserSignedUpEvent {

    private final Long userId;
    private final String email;
    private final String nickname;
    private final LocalDateTime signedUpAt;

    public static UserSignedUpEvent of(UserEntity user) {
        return new UserSignedUpEvent(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                LocalDateTime.now()
        );
    }

}