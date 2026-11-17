package com.kaya.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPaymentPasswordRequest(

        @NotBlank(message = "请输入登录密码")
        String loginPassword,

        @NotBlank(message = "请输入新的支付密码")
        @Pattern(regexp = "^[0-9]{6}$", message = "支付密码需为 6 位数字")
        String newPassword
) {
}
