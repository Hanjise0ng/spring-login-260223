package com.han.back.global.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * UUID v7 구조 (128 bit):
 * - 상위 48 bit: Unix timestamp (밀리초)
 * - 4 bit: 버전 (0111 = 7)
 * - 12 bit: 랜덤 (rand_a)
 * - 2 bit: 변형 (10)
 * - 62 bit: 랜덤 (rand_b)

 * 장점:
 * - 생성 시간 기반 자연 정렬 (사전순 = 시간순)
 * - B-tree 인덱스 지역성 향상
 * - 세션 생성 시각을 ID 자체에서 추론 가능
 */
public final class UuidUtil {

    private UuidUtil() {}

    private static final SecureRandom RANDOM = new SecureRandom();

    public static UUID generate() {
        long timestamp = System.currentTimeMillis();
        long randomBits = RANDOM.nextLong();

        // MSB: 48-bit timestamp | 4-bit version(7) | 12-bit rand_a
        long msb = (timestamp & 0x0000_FFFF_FFFF_FFFFL) << 16;
        msb |= 0x7000L;
        msb |= (randomBits >>> 52) & 0x0FFFL;

        // LSB: 2-bit variant(10) | 62-bit rand_b
        long lsb = (randomBits & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;

        return new UUID(msb, lsb);
    }

    public static String generateString() {
        return generate().toString();
    }

}