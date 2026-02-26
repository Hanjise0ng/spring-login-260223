package com.han.back.domain.auth.service.implement;

import com.han.back.domain.auth.dto.request.SignUpRequestDto;
import com.han.back.domain.auth.service.AuthService;
import com.han.back.domain.user.entity.UserEntity;
import com.han.back.domain.user.mapper.UserMapper;
import com.han.back.domain.user.repository.UserRepository;
import com.han.back.global.dto.BaseResponse;
import com.han.back.global.dto.BaseResponseStatus;
import com.han.back.global.dto.Empty;
import com.han.back.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResponseEntity<BaseResponse<Empty>> signUp(SignUpRequestDto dto) {
        String userId = dto.getUserId();

        if (userRepository.existsByUserId(userId)) {
            throw new CustomException(BaseResponseStatus.DUPLICATE_ID);
        }

        dto.setPassword(passwordEncoder.encode(dto.getPassword()));

        UserEntity user = userMapper.ofSignupDTO(dto);

        userRepository.save(user);

        return BaseResponse.success();
    }

}