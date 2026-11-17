package com.kaya.dto.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferRecipientInput(
        @NotBlank(message = "请输入收款人姓名")
        @Size(max = 64, message = "收款人姓名过长")
        String displayName,

        @NotBlank(message = "请输入收款账号")
        @Size(max = 64, message = "收款账号过长")
        String accountNo,

        @Size(max = 64, message = "银行名称过长")
        String bankName
) {
}
