package com.han.back.controller;

import com.han.back.controller.docs.AuthApiDocs;
import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.dto.response.LoginIdCheckResponseDto;
import com.han.back.domain.auth.exception.AuthResponseStatus;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.global.exception.CustomException;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.token.AuthToken;
import com.han.back.global.security.token.transport.TokenTransportResolver;
import com.han.back.global.security.token.util.AuthHttpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApiDocs {

    private final AuthService authService;
    private final TokenTransportResolver tokenTransportResolver;

    @Override
    @GetMapping("/check-login-id")
    public ResponseEntity<BaseResponse<LoginIdCheckResponseDto>> checkLoginId(
            @RequestParam @NotBlank String loginId) {

        LoginIdCheckResponseDto response = authService.checkLoginId(loginId);
        return BaseResponse.success(response);
    }

    @Override
    @PostMapping("/sign-up")
    public ResponseEntity<BaseResponse<Empty>> signUp(
            @RequestBody @Valid SignUpRequestDto requestBody) {

        authService.signUp(requestBody);
        return BaseResponse.success();
    }

    @Override
    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<Empty>> reissue(
            HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = AuthHttpUtil.extractRefreshToken(request)
                .orElseThrow(() -> new CustomException(AuthResponseStatus.AUTH_REFRESH_TOKEN_MISSING));
        AuthToken newTokens = authService.reissue(refreshToken);

        tokenTransportResolver.resolve(request).write(response, newTokens);
        return BaseResponse.success();
    }

}