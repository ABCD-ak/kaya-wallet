package com.kaya.dto.user;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 只传需要修改的字段；null 表示不修改。
 * 邮箱是登录凭证，修改需要单独的新邮箱验证流程，不在此接口内。
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 64, message = "昵称长度需为 1-64 位")
        String displayName,

        @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "用户名需为 3-32 位字母、数字或下划线")
        String username,

        @Pattern(regexp = "^(male|female|other)$", message = "性别取值不正确")
        String gender,

        @Past(message = "生日必须早于今天")
        LocalDate birthday,

        String email
) {
}
