package com.han.back.controller;

import com.han.back.domain.auth.dto.OAuth2CodePayload;
import com.han.back.domain.auth.dto.SignInResult;
import com.han.back.domain.auth.dto.request.OAuth2SignUpCompleteRequestDto;
import com.han.back.domain.auth.oauth2.service.OAuth2CodeStore;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.device.dto.DeviceInfo;
import com.han.back.global.device.DeviceInfoResolver;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.transport.TokenTransport;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Auth", description = "소셜 인증 API")
public class SocialAuthController {

    private final AuthService authService;
    private final DeviceInfoResolver deviceInfoResolver;
    private final TokenTransportResolver tokenTransportResolver;
    private final OAuth2CodeStore oauth2CodeStore;

    @GetMapping("/token")
    public ResponseEntity<BaseResponse<Empty>> exchangeOAuth2Code(
            @RequestParam String code,
            HttpServletRequest request, HttpServletResponse response) {

        OAuth2CodePayload payload = oauth2CodeStore.consume(code);

        TokenTransport transport = tokenTransportResolver.resolve(request);
        transport.write(response, AuthToken.of(payload.getAccessToken(), payload.getRefreshToken()));
        transport.writeDeviceCookie(response, payload.getDeviceFingerprint());

        return BaseResponse.success();
    }

    @PostMapping("/complete")
    public ResponseEntity<BaseResponse<Map<String, String>>> completeSocialSignUp(
            @RequestBody @Valid OAuth2SignUpCompleteRequestDto request,
            HttpServletRequest httpRequest) {

        DeviceInfo deviceInfo = deviceInfoResolver.resolve(httpRequest);

        SignInResult signInResult = authService.completeSocialSignUp(
                request.getTempToken(), request.getEmail(), deviceInfo);
        String code = oauth2CodeStore.save(signInResult);

        return BaseResponse.success(Map.of("code", code));
    }

}