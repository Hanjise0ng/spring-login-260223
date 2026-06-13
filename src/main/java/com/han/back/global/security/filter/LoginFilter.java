package com.han.back.global.security.filter;

import com.han.back.domain.auth.dto.request.SignInRequestDto;
import com.han.back.global.exception.CustomAuthenticationException;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.util.ClientIpResolver;
import com.han.back.global.util.SecurityPathConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final ObjectMapper objectMapper;

    public LoginFilter(AuthenticationManager authenticationManager,
                       ObjectMapper objectMapper,
                       AuthenticationSuccessHandler successHandler,
                       AuthenticationFailureHandler failureHandler) {
        super.setAuthenticationManager(authenticationManager);
        this.objectMapper = objectMapper;
        setAuthenticationSuccessHandler(successHandler);
        setAuthenticationFailureHandler(failureHandler);
        setFilterProcessesUrl(SecurityPathConst.LOGIN_PATH);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        try {
            SignInRequestDto dto = objectMapper.readValue(request.getInputStream(), SignInRequestDto.class);

            String loginId = dto.getLoginId();
            String clientIp = ClientIpResolver.resolve(request);

            MDC.put("loginId", loginId);
            log.info("Login Attempt - LoginId: {} | ClientIP: {}", loginId, clientIp);

            return this.getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(loginId, dto.getPassword())
            );
        } catch (IOException e) {
            log.error("Login Request Parsing Error - ClientIP: {}", ClientIpResolver.resolve(request), e);
            throw new CustomAuthenticationException(ResponseStatus.MALFORMED_REQUEST_BODY);
        }
    }

}