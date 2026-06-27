package com.han.back.domain.user.entity;

import com.han.back.global.entity.BaseTime;
import com.han.back.global.util.UuidUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_nickname_tag", columnNames = {"nickname", "tag"})
        },
        indexes = {
                @Index(name = "idx_users_nickname_tag", columnList = "nickname, tag")
        }
)
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "user_id"))
})
public class UserEntity extends BaseTime {

    @Builder.Default
    @Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 36)
    private String publicId = UuidUtil.generateString();

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, length = 4)
    private String tag;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateNicknameAndTag(String nickname, String tag) {
        this.nickname = nickname;
        this.tag = tag;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

}