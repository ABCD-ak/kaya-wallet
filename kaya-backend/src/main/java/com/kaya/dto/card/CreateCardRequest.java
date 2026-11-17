package com.kaya.dto.card;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCardRequest(
        @NotBlank(message = "请选择卡类型")
        @Pattern(regexp = "^(debit|credit)$", message = "卡类型不正确")
        String cardType,

        @NotBlank(message = "请选择卡组织/发卡行")
        @Pattern(regexp = "^(visa|mastercard|unionpay|icbc|abc|boc|ccb|bocom|psbc)$", message = "卡组织/发卡行不正确")
        String brand,

        @NotBlank(message = "请输入卡号后四位")
        @Pattern(regexp = "^[0-9]{4}$", message = "卡号后四位需为 4 位数字")
        String last4,

        @NotBlank(message = "请输入持卡人姓名")
        @Size(max = 64, message = "持卡人姓名过长")
        String holderName,

        @NotNull(message = "请选择有效期月份")
        @Min(value = 1, message = "有效期月份不正确")
        @Max(value = 12, message = "有效期月份不正确")
        Integer expiryMonth,

        @NotNull(message = "请选择有效期年份")
        @Min(value = 2000, message = "有效期年份不正确")
        Integer expiryYear,

        @NotBlank(message = "请选择币种")
        @Pattern(regexp = "^[A-Z]{3}$", message = "币种需为 3 位大写字母")
        String currencyCode,

        @Size(max = 191, message = "支付令牌过长")
        String paymentToken,

        Boolean isPrimary
) {
}
