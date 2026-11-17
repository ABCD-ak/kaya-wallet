package com.kaya.dto.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TransferQuoteRequest(
        @NotBlank(message = "请选择钱包")
        @Pattern(regexp = "^[0-9]+$", message = "钱包 ID 不正确")
        String walletId,

        @Pattern(regexp = "^[0-9]+$", message = "银行卡 ID 不正确")
        String cardId,

        @Pattern(regexp = "^[0-9]+$", message = "联系人 ID 不正确")
        String recipientId,

        @Size(max = 64, message = "收款账号过长")
        String accountNo,

        @NotBlank(message = "请输入金额")
        @Pattern(regexp = "^[0-9]+\\.[0-9]{2}$", message = "金额需为两位小数的正数")
        String amount
) {
}
