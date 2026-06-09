package com.han.back.global.security.login;

import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.device.DeviceInfoProvider;
import com.han.back.global.response.ResponseStatus;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import com.han.back.global.security.token.util.AuthHttpUtil;
import com.han.back.global.util.HttpResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultLoginSuccessProcessor implements LoginSuccessProcessor {

    private final AuthService authService;
    private final DeviceInfoProvider deviceInfoProvider;
    private final TokenTransportResolver tokenTransportResolver;
    private final HttpResponseUtil httpResponseUtil;

    @Override
    public void process(HttpServletRequest request,
                        HttpServletResponse response,
                        Authentication authentication) {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        AuthToken previousTokens = AuthHttpUtil.extractTokenPairLeniently(request);

        DeviceInfo deviceInfo = deviceInfoProvider.get(request);
        SignInResult result = authService.completeSignIn(userDetails, deviceInfo, previousTokens);

        TokenTransport transport = tokenTransportResolver.resolve(request);
        transport.write(response, result.getTokens());
        transport.writeDeviceCookie(response, result.getDeviceFingerprint());
        httpResponseUtil.writeResponse(response, ResponseStatus.SUCCESS);
    }

}