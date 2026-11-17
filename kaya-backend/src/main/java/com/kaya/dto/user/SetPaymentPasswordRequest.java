package com.kaya.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SetPaymentPasswordRequest(

        // 已设置过支付密码时必填，用于验证旧支付密码
        @Pattern(regexp = "^[0-9]{6}$", message = "支付密码需为 6 位数字")
        String currentPassword,

        @NotBlank(message = "请输入新的支付密码")
        @Pattern(regexp = "^[0-9]{6}$", message = "支付密码需为 6 位数字")
        String newPassword
) {
}
