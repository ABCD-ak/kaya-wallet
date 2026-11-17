package com.kaya.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequest(
        @NotBlank(message = "缺少刷新令牌")
        String refreshToken
) {
}
