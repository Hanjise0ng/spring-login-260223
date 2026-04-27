package com.han.back.global.security.handler;

import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.util.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomAccessDeniedHandlerTest {

    @Mock private HttpResponseUtil httpResponseUtil;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Test
    @DisplayName("인가 실패 시 NO_PERMISSION 응답을 작성한다")
    void handle_AccessDenied() {
        // given
        AccessDeniedException exception = new AccessDeniedException("Access Denied");

        // when
        customAccessDeniedHandler.handle(request, response, exception);

        // then
        verify(httpResponseUtil).writeResponse(response, BaseResponseStatus.NO_PERMISSION);
    }

}