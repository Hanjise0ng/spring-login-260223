package com.han.back.global.security.filter;

import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponseStatus;
import com.han.back.global.util.HttpResponseUtil;
import jakarta.servlet.FilterChain;
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

import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtExceptionFilterTest {

    @Mock private HttpResponseUtil httpResponseUtil;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtExceptionFilter filter;

    @Test
    @DisplayName("예외가 발생하지 않으면 필터 체인을 정상 진행한다")
    void doFilter_Success() throws Exception {
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("CustomAuthenticationException 발생 시 지정된 상태로 응답을 작성한다")
    void doFilter_CustomAuthenticationException() throws Exception {
        willThrow(new CustomAuthenticationException(BaseResponseStatus.AUTHENTICATION_FAIL))
                .given(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(httpResponseUtil).writeResponse(response, BaseResponseStatus.AUTHENTICATION_FAIL);
    }

    @Test
    @DisplayName("CustomException 발생 시 지정된 상태로 응답을 작성한다")
    void doFilter_CustomException() throws Exception {
        willThrow(new CustomException(BaseResponseStatus.REDIS_ERROR))
                .given(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(httpResponseUtil).writeResponse(response, BaseResponseStatus.REDIS_ERROR);
    }

    @Test
    @DisplayName("예상치 못한 RuntimeException 발생 시 INTERNAL_SERVER_ERROR를 응답한다")
    void doFilter_GeneralException() throws Exception {
        willThrow(new RuntimeException("Unexpected Error"))
                .given(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(httpResponseUtil).writeResponse(response, BaseResponseStatus.INTERNAL_SERVER_ERROR);
    }

}