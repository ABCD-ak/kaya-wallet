package com.kaya.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "重置令牌不能为空")
        @Size(max = 128, message = "重置令牌不正确")
        String token,

        @NotBlank(message = "请输入新密码")
        @Size(min = 8, max = 64, message = "密码长度需为 8-64 位")
        String newPassword
) {
}
