package com.han.back.controller;

import com.han.back.controller.docs.CredentialLinkApiDocs;
import com.han.back.domain.auth.credential.dto.request.LocalCredentialCreateRequestDto;
import com.han.back.domain.auth.credential.dto.response.LinkedCredentialResponseDto;
import com.han.back.domain.auth.credential.dto.response.SocialLinkStartResponseDto;
import com.han.back.domain.auth.credential.service.CredentialLinkService;
import com.han.back.domain.user.entity.AuthProvider;
import com.han.back.global.response.BaseResponse;
import com.han.back.global.response.Empty;
import com.han.back.global.security.principal.CustomUserDetails;
import com.han.back.global.security.token.util.SocialLinkTokenUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth/credentials")
@RequiredArgsConstructor
public class CredentialLinkController implements CredentialLinkApiDocs {

    private final CredentialLinkService credentialLinkService;
    private final SocialLinkTokenUtil socialLinkTokenUtil;

    @Override
    @GetMapping("/socials")
    public ResponseEntity<BaseResponse<List<LinkedCredentialResponseDto>>> getLinkedSocials(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return BaseResponse.success(credentialLinkService.getLinkedSocials(userDetails.getId()));
    }

    @Override
    @DeleteMapping("/socials/{provider}")
    public ResponseEntity<BaseResponse<Empty>> unlinkSocial(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable AuthProvider provider) {

        credentialLinkService.unlinkSocialCredential(userDetails.getId(), provider);
        return BaseResponse.success();
    }

    @Override
    @PostMapping("/local")
    public ResponseEntity<BaseResponse<Empty>> promoteToLocal(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid LocalCredentialCreateRequestDto request) {

        credentialLinkService.promoteToLocalAccount(userDetails.getId(), request);
        return BaseResponse.success();
    }

    @Override
    @PostMapping("/socials/link/start")
    public ResponseEntity<BaseResponse<SocialLinkStartResponseDto>> startSocialLink(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String linkToken = socialLinkTokenUtil.issue(userDetails.getId());
        return BaseResponse.success(SocialLinkStartResponseDto.of(linkToken));
    }

}