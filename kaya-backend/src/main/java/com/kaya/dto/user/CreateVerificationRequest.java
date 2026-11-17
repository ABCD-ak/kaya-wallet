package com.kaya.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateVerificationRequest(
        @NotBlank(message = "请选择认证类型")
        @Pattern(regexp = "^(identity|address|phone|email)$", message = "认证类型不正确")
        String type,

        @Size(max = 32, message = "证件类型过长")
        String documentType,

        @Size(max = 64, message = "证件号过长")
        String documentNo
) {
}
