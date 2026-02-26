package com.han.back.controller;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.Empty;
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
        return authService.signUp(requestBody);
    }

}