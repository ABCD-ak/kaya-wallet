package com.kaya.dto.auth;

import com.kaya.dto.user.UserResponse;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
