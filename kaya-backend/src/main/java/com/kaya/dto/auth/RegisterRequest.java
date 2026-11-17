package com.kaya.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "请输入姓名")
        @Size(max = 64, message = "姓名最多 64 个字符")
        String displayName,

        @NotBlank(message = "请输入邮箱")
        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱最多 254 个字符")
        String email,

        @NotBlank(message = "请输入密码")
        @Size(min = 8, max = 64, message = "密码长度需为 8-64 位")
        String password,

        @Size(min = 3, max = 32, message = "用户名长度需为 3-32 位")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        String username
) {
}
