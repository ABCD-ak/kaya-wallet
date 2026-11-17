package com.kaya.dto.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public record BudgetResponse(
        String id,
        String categoryId,
        String categoryName,
        String categoryIcon,
        String categoryColor,
        String periodType,
        LocalDate periodStart,
        LocalDate periodEnd,
        String amountLimit,
        String spent,
        String remaining,
        double usedPercent,
        String status,
        double alertThreshold,
        String currencyCode
) {

    public static BudgetResponse of(
            Long id,
            Long categoryId,
            String categoryName,
            String categoryIcon,
            String categoryColor,
            String periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal amountLimit,
            BigDecimal spent,
            BigDecimal alertThreshold,
            String currencyCode) {
        BigDecimal limit = amountLimit.setScale(2, RoundingMode.HALF_UP);
        BigDecimal used = spent == null ? BigDecimal.ZERO : spent.setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = limit.subtract(used).setScale(2, RoundingMode.HALF_UP);
        double usedPercent = limit.signum() == 0
                ? 0
                : used.multiply(BigDecimal.valueOf(100))
                        .divide(limit, 2, RoundingMode.HALF_UP)
                        .doubleValue();
        String status = used.compareTo(limit) > 0
                ? "over_budget"
                : used.compareTo(limit) == 0 ? "reach_limit" : "under_control";

        return new BudgetResponse(
                String.valueOf(id),
                String.valueOf(categoryId),
                categoryName,
                categoryIcon,
                categoryColor,
                periodType,
                periodStart,
                periodEnd,
                limit.toPlainString(),
                used.toPlainString(),
                remaining.toPlainString(),
                usedPercent,
                status,
                alertThreshold.setScale(2, RoundingMode.HALF_UP).doubleValue(),
                currencyCode);
    }
}
