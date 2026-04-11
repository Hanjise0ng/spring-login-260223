package com.han.back.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NoArgsConstructor;

@Schema(description = "데이터 없음을 나타내는 빈 객체 (응답 데이터가 없는 경우 사용)")
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Empty {
    private static final Empty INSTANCE = new Empty();
    public static Empty getInstance() {
        return INSTANCE;
    }
}