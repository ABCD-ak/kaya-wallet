package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.user.AvatarResponse;
import com.kaya.dto.user.ChangePasswordRequest;
import com.kaya.dto.user.PaymentPasswordStatusResponse;
import com.kaya.dto.user.ResetPaymentPasswordRequest;
import com.kaya.dto.user.SetPaymentPasswordRequest;
import com.kaya.dto.user.UpdateProfileRequest;
import com.kaya.dto.user.UserResponse;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.AuthService;
import com.kaya.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(authService.getCurrentUser(principal.id()));
    }

    @PatchMapping("/me")
    public ApiResponse<UserResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(userService.updateProfile(principal.id(), request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        userService.changePassword(principal.id(), principal.sessionId(), request);
        return ApiResponse.ok(null);
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AvatarResponse> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(userService.updateAvatar(principal.id(), file));
    }

    @GetMapping("/me/payment-password")
    public ApiResponse<PaymentPasswordStatusResponse> paymentPasswordStatus(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(userService.getPaymentPasswordStatus(principal.id()));
    }

    @PostMapping("/me/payment-password")
    public ApiResponse<Void> setPaymentPassword(
            @Valid @RequestBody SetPaymentPasswordRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        userService.setPaymentPassword(principal.id(), request);
        return ApiResponse.ok(null);
    }

    @PostMapping("/me/payment-password/reset")
    public ApiResponse<Void> resetPaymentPassword(
            @Valid @RequestBody ResetPaymentPasswordRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        userService.resetPaymentPassword(principal.id(), request);
        return ApiResponse.ok(null);
    }
}
