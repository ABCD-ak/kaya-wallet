package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.user.UpdateSettingsRequest;
import com.kaya.dto.user.UserSettingsResponse;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/me/settings")
public class UserSettingsController {

    private final UserSettingsService userSettingsService;

    public UserSettingsController(UserSettingsService userSettingsService) {
        this.userSettingsService = userSettingsService;
    }

    @GetMapping
    public ApiResponse<UserSettingsResponse> get(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(userSettingsService.get(principal.id()));
    }

    @PatchMapping
    public ApiResponse<UserSettingsResponse> update(
            @Valid @RequestBody UpdateSettingsRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(userSettingsService.update(principal.id(), request));
    }
}
