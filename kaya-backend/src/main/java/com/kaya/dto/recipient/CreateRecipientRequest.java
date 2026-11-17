package com.kaya.dto.recipient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecipientRequest(
        @NotBlank(message = "请输入收款人姓名")
        @Size(max = 64, message = "收款人姓名过长")
        String displayName,

        @Size(max = 32, message = "手机号过长")
        String phone,

        @Email(message = "邮箱格式不正确")
        @Size(max = 254, message = "邮箱过长")
        String email,

        @Size(max = 64, message = "收款账号过长")
        String accountNo,

        @Size(max = 64, message = "银行名称过长")
        String bankName,

        @Size(max = 512, message = "头像地址过长")
        String avatarUrl,

        Boolean isFavorite
) {
}
