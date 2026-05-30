package com.han.back.global.infra.redis.util;

import com.han.back.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisUtilTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisUtil redisUtil;

    @BeforeEach
    void setUp() {
        redisUtil = new RedisUtil(redisTemplate);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Test
    @DisplayName("setDataExpire - 정상 처리")
    void setDataExpire_Success() {
        // when
        redisUtil.setDataExpire("testKey", "testValue", Duration.ofSeconds(5));

        // then
        verify(valueOperations).set("testKey", "testValue", Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("setDataExpire - 예외 발생 시 REDIS_ERROR CustomException을 던진다")
    void setDataExpire_Exception() {
        // given
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection error"));

        // when & then
        assertThatThrownBy(() -> redisUtil.setDataExpire("testKey", "testValue", Duration.ofSeconds(5)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("setIfAbsent - 정상적으로 저장되면 true를 반환한다")
    void setIfAbsent_Success() {
        // given
        given(valueOperations.setIfAbsent("testKey", "testValue", Duration.ofSeconds(5)))
                .willReturn(true);

        // when
        boolean result = redisUtil.setIfAbsent("testKey", "testValue", Duration.ofSeconds(5));

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("setIfAbsent - 예외 발생 시 REDIS_ERROR CustomException을 던진다")
    void setIfAbsent_Exception() {
        // given
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException());

        // when & then
        assertThatThrownBy(() -> redisUtil.setIfAbsent("testKey", "testValue", Duration.ofSeconds(5)))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getData - 데이터가 존재하면 Optional에 담아 반환한다")
    void getData_Success() {
        // given
        given(valueOperations.get("testKey")).willReturn("testValue");

        // when
        Optional<String> result = redisUtil.getData("testKey");

        // then
        assertThat(result).isPresent().contains("testValue");
    }

    @Test
    @DisplayName("getData - 예외 발생 시 REDIS_ERROR CustomException을 던진다")
    void getData_Exception() {
        // given
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException());

        // when & then
        assertThatThrownBy(() -> redisUtil.getData("testKey"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("deleteData - 정상 처리")
    void deleteData_Success() {
        // when
        redisUtil.deleteData("testKey");

        // then
        verify(redisTemplate).delete("testKey");
    }

    @Test
    @DisplayName("deleteData - 예외 발생 시 REDIS_ERROR CustomException을 던진다")
    void deleteData_Exception() {
        // given
        given(redisTemplate.delete("testKey")).willThrow(new RuntimeException());

        // when & then
        assertThatThrownBy(() -> redisUtil.deleteData("testKey"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("hasKey - 키가 존재하면 true를 반환한다")
    void hasKey_Success() {
        // given
        given(redisTemplate.hasKey("testKey")).willReturn(true);

        // when
        boolean result = redisUtil.hasKey("testKey");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasKey - 예외 발생 시 REDIS_ERROR CustomException을 던진다")
    void hasKey_Exception() {
        // given
        given(redisTemplate.hasKey("testKey")).willThrow(new RuntimeException());

        // when & then
        assertThatThrownBy(() -> redisUtil.hasKey("testKey"))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("getAndDelete - 키가 존재하면 값을 Optional에 담아 반환한다")
    void getAndDelete_Success() {
        // given
        given(valueOperations.getAndDelete("testKey")).willReturn("testValue");

        // when
        Optional<String> result = redisUtil.getAndDelete("testKey");

        // then
        assertThat(result).isPresent().contains("testValue");
    }

    @Test
    @DisplayName("getAndDelete - 키가 없으면 Optional.empty()를 반환한다")
    void getAndDelete_Empty() {
        // given
        given(valueOperations.getAndDelete("testKey")).willReturn(null);

        // when
        Optional<String> result = redisUtil.getAndDelete("testKey");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAndDelete - 예외 발생 시 REDIS_ERROR CustomException을 던진다")
    void getAndDelete_Exception() {
        // given
        given(redisTemplate.opsForValue()).willThrow(new RuntimeException("Redis connection error"));

        // when & then
        assertThatThrownBy(() -> redisUtil.getAndDelete("testKey"))
                .isInstanceOf(CustomException.class);
    }

}