package com.han.back.global.security.filter;

import com.han.back.domain.auth.dto.request.SignInRequestDto;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.security.login.LoginFailureRateLimiter;
import com.han.back.global.util.ClientIpResolver;
import com.han.back.global.util.HttpResponseUtil;
import com.han.back.global.util.SecurityPathConst;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginFailureRateLimiter rateLimiter;
    private final HttpResponseUtil httpResponseUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(request);

        String loginId = extractLoginId(cachedRequest);
        String clientIp = ClientIpResolver.resolve(cachedRequest);

        if (rateLimiter.isBlocked(loginId, clientIp)) {
            log.warn("Login blocked by rate limit - loginId: {} | ip: {}", loginId, clientIp);
            httpResponseUtil.writeResponse(response, ResponseStatus.RATE_LIMIT_EXCEEDED);
            return;
        }

        filterChain.doFilter(cachedRequest, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && SecurityPathConst.LOGIN_PATH.equals(request.getRequestURI());
    }

    private String extractLoginId(CachedBodyRequestWrapper request) {
        try {
            SignInRequestDto dto = objectMapper.readValue(request.getCachedBody(), SignInRequestDto.class);
            return dto.getLoginId();
        } catch (Exception e) {
            return "UNIDENTIFIED";
        }
    }

}