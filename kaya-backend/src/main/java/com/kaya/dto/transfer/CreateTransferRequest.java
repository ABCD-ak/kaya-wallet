package com.kaya.dto.transfer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTransferRequest(
        @NotBlank(message = "请选择钱包")
        @Pattern(regexp = "^[0-9]+$", message = "钱包 ID 不正确")
        String walletId,

        @Pattern(regexp = "^[0-9]+$", message = "银行卡 ID 不正确")
        String cardId,

        @Pattern(regexp = "^[0-9]+$", message = "联系人 ID 不正确")
        String recipientId,

        @Valid
        TransferRecipientInput recipient,

        @NotBlank(message = "请输入金额")
        @Pattern(regexp = "^[0-9]+\\.[0-9]{2}$", message = "金额需为两位小数的正数")
        String amount,

        @Size(max = 255, message = "备注不能超过 255 字")
        String note,

        // 已设置支付密码的用户转账时必填
        @Pattern(regexp = "^[0-9]{6}$", message = "支付密码需为 6 位数字")
        String paymentPassword
) {
}
