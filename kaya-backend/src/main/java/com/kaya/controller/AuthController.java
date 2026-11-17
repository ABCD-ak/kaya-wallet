package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.auth.AuthTokens;
import com.kaya.dto.auth.EmailCodeRequest;
import com.kaya.dto.auth.EmailLoginRequest;
import com.kaya.dto.auth.ForgotPasswordRequest;
import com.kaya.dto.auth.LoginRequest;
import com.kaya.dto.auth.LogoutRequest;
import com.kaya.dto.auth.RefreshRequest;
import com.kaya.dto.auth.RegisterRequest;
import com.kaya.dto.auth.ResetPasswordRequest;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.AuthService;
import com.kaya.service.EmailCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailCodeService emailCodeService;

    public AuthController(AuthService authService, EmailCodeService emailCodeService) {
        this.authService = authService;
        this.emailCodeService = emailCodeService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthTokens> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.register(
                request,
                httpRequest.getHeader("User-Agent"),
                clientIp(httpRequest)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokens> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(
                request,
                httpRequest.getHeader("User-Agent"),
                clientIp(httpRequest)));
    }

    @PostMapping("/email-code")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> sendEmailCode(
            @Valid @RequestBody EmailCodeRequest request,
            HttpServletRequest httpRequest) {
        emailCodeService.sendLoginCode(request.email(), clientIp(httpRequest));
        return ApiResponse.ok(null);
    }

    @PostMapping("/login/email")
    public ApiResponse<AuthTokens> loginWithEmailCode(
            @Valid @RequestBody EmailLoginRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.loginWithEmailCode(
                request,
                httpRequest.getHeader("User-Agent"),
                clientIp(httpRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokens> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.refresh(
                request.refreshToken(),
                httpRequest.getHeader("User-Agent"),
                clientIp(httpRequest)));
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ApiResponse.ok(null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.ok(null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser user
                ? user
                : null;
        authService.logout(request == null ? null : request.refreshToken(), principal);
        return ApiResponse.ok(null);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
