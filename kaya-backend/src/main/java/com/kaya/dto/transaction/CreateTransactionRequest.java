package com.kaya.dto.transaction;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Jackson requires explicit record annotations for constructor-property mapping
 * when the ObjectMapper is not auto-configured for Java record reflection.
 */
public record CreateTransactionRequest(
        @NotBlank(message = "请选择钱包")
        @Pattern(regexp = "^[0-9]+$", message = "钱包 ID 不正确")
        @JsonProperty("walletId") String walletId,

        @NotBlank(message = "请选择收支类型")
        @Pattern(regexp = "^(income|expense)$", message = "收支类型不正确")
        @JsonProperty("type") String type,

        @NotBlank(message = "请输入金额")
        @Pattern(regexp = "^[0-9]+\\.[0-9]{2}$", message = "金额需为两位小数的正数")
        @JsonProperty("amount") String amount,

        @NotBlank(message = "请选择分类")
        @Pattern(regexp = "^[0-9]+$", message = "分类 ID 不正确")
        @JsonProperty("categoryId") String categoryId,

        @Size(max = 255, message = "备注不能超过 255 字")
        @JsonProperty("note") String note,

        @JsonProperty("occurredAt") String occurredAt
) {

    @JsonCreator
    public static CreateTransactionRequest create(
            @JsonProperty("walletId") String walletId,
            @JsonProperty("type") String type,
            @JsonProperty("amount") String amount,
            @JsonProperty("categoryId") String categoryId,
            @JsonProperty("note") String note,
            @JsonProperty("occurredAt") String occurredAt) {
        return new CreateTransactionRequest(walletId, type, amount, categoryId, note, occurredAt);
    }
}
