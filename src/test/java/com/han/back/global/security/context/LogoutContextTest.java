package com.han.back.global.security.context;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LogoutContextTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("결과를 세팅하면 request attribute에 저장된다")
    void setResult() {
        LogoutContext.setResult(request, LogoutContext.Result.SUCCESS);
        verify(request).setAttribute(eq("logout.result"), eq(LogoutContext.Result.SUCCESS));
    }

    @Test
    @DisplayName("Attribute에 Result 타입이 있으면 해당 값을 반환한다")
    void getResult_Success() {
        given(request.getAttribute("logout.result")).willReturn(LogoutContext.Result.REDIS_ERROR);

        LogoutContext.Result result = LogoutContext.getResult(request);

        assertThat(result).isEqualTo(LogoutContext.Result.REDIS_ERROR);
    }

    @Test
    @DisplayName("Attribute가 없거나 잘못된 타입이면 UNAUTHENTICATED를 반환한다")
    void getResult_NullOrInvalidType() {
        given(request.getAttribute("logout.result")).willReturn("WrongType");

        LogoutContext.Result result = LogoutContext.getResult(request);

        assertThat(result).isEqualTo(LogoutContext.Result.UNAUTHENTICATED);
    }

}