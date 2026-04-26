package com.han.back.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MaskingUtil")
class MaskingUtilTest {

    @Test
    @DisplayName("이메일 — 로컬파트 3자 이상이면 앞 2자만 노출한다")
    void email_normalLength_masksCorrectly() {
        assertThat(MaskingUtil.maskTarget("test@example.com"))
                .isEqualTo("te***@example.com");
    }

    @Test
    @DisplayName("이메일 — 로컬파트 2자 이하면 전부 마스킹한다")
    void email_shortLocal_masksAll() {
        assertThat(MaskingUtil.maskTarget("ab@example.com"))
                .isEqualTo("***@example.com");
    }

    @Test
    @DisplayName("전화번호 — 앞 3자 + 뒤 4자만 노출한다")
    void phone_masksMiddle() {
        assertThat(MaskingUtil.maskTarget("01012345678"))
                .isEqualTo("010****5678");
    }

    @Test
    @DisplayName("짧은 값(4자 이하) — 전부 마스킹한다")
    void shortValue_masksAll() {
        assertThat(MaskingUtil.maskTarget("abc"))
                .isEqualTo("****");
    }

    @Test
    @DisplayName("null — 'null' 문자열을 반환한다")
    void nullInput_returnsNullString() {
        assertThat(MaskingUtil.maskTarget(null))
                .isEqualTo("null");
    }

}