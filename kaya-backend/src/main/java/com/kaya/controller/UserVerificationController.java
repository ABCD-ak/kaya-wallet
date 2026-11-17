package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.user.CreateVerificationRequest;
import com.kaya.dto.user.VerificationResponse;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users/me/verifications")
public class UserVerificationController {

    private final VerificationService verificationService;

    public UserVerificationController(VerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @GetMapping
    public ApiResponse<List<VerificationResponse>> list(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(verificationService.list(principal.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<VerificationResponse> submit(
            @Valid @RequestBody CreateVerificationRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(verificationService.submit(principal.id(), request));
    }
}
