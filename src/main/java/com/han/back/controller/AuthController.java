package com.han.back.controller;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.dto.Empty;
import com.han.back.global.exception.CustomException;
import com.han.back.global.security.dto.AuthTokenDto;
import com.han.back.global.security.util.AuthHttpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/sign-up")
    public ResponseEntity<BaseResponse<Empty>> signUp(
            @RequestBody @Valid SignUpRequestDto requestBody
    ) {
        authService.signUp(requestBody);
        return BaseResponse.success();
    }

    @PostMapping("/reissue")
    public ResponseEntity<BaseResponse<Empty>> reissue(
            HttpServletRequest request, HttpServletResponse response) {

        String oldAccessToken = AuthHttpUtil.extractAccessToken(request)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.MISSING_ACCESS_TOKEN));
        String oldRefreshToken = AuthHttpUtil.extractRefreshToken(request)
                .orElseThrow(() -> new CustomException(BaseResponseStatus.MISSING_REFRESH_TOKEN));

        AuthTokenDto token = authService.reissue(
                AuthTokenDto.of(oldAccessToken, oldRefreshToken)
        );

        AuthHttpUtil.setTokenResponse(request, response, token);
        return BaseResponse.success();
    }

}