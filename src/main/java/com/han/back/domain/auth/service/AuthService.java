package com.han.back.domain.auth.service;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.Empty;
import org.springframework.http.ResponseEntity;

public interface AuthService {

    ResponseEntity<BaseResponse<Empty>> signUp(SignUpRequestDto dto);

}