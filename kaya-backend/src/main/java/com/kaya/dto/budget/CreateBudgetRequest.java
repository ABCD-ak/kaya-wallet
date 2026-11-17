package com.kaya.dto.budget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CreateBudgetRequest(
        @NotBlank(message = "请选择分类")
        @Pattern(regexp = "^[0-9]+$", message = "分类 ID 不正确")
        String categoryId,

        @NotBlank(message = "请选择月份")
        @Pattern(regexp = "^[0-9]{4}-[0-9]{2}$", message = "月份格式需为 YYYY-MM")
        String month,

        @NotBlank(message = "请输入预算金额")
        @Pattern(regexp = "^[0-9]+\\.[0-9]{2}$", message = "金额需为两位小数的正数")
        String amountLimit,

        @DecimalMin(value = "0", message = "提醒阈值不能小于 0")
        @DecimalMax(value = "100", message = "提醒阈值不能大于 100")
        BigDecimal alertThreshold
) {
}
