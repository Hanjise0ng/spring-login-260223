package com.han.back.domain.auth.credential.service;

import com.han.back.domain.auth.credential.dto.request.LocalCredentialCreateRequestDto;
import com.han.back.domain.auth.credential.dto.response.LinkedCredentialResponseDto;
import com.han.back.domain.user.entity.AuthProvider;

import java.util.List;

public interface CredentialLinkService {

    void unlinkSocialCredential(Long userId, AuthProvider provider);

    List<LinkedCredentialResponseDto> getLinkedSocials(Long userId);

    void promoteToLocalAccount(Long userId, LocalCredentialCreateRequestDto request);

}