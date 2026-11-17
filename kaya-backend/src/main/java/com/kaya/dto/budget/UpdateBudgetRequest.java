package com.kaya.dto.budget;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record UpdateBudgetRequest(
        @Pattern(regexp = "^[0-9]+\\.[0-9]{2}$", message = "金额需为两位小数的正数")
        String amountLimit,

        @DecimalMin(value = "0", message = "提醒阈值不能小于 0")
        @DecimalMax(value = "100", message = "提醒阈值不能大于 100")
        BigDecimal alertThreshold
) {
}
