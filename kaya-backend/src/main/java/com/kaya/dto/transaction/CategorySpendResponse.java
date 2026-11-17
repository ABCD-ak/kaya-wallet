package com.kaya.dto.transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record CategorySpendResponse(
        String categoryId,
        String categoryName,
        String categoryIcon,
        String categoryColor,
        String amount,
        double percentage,
        int transactionCount
) {

    public static CategorySpendResponse from(CategorySpendRow row, BigDecimal totalSpent) {
        BigDecimal amount = row.getAmount() == null ? BigDecimal.ZERO : row.getAmount();
        double percentage = totalSpent == null || totalSpent.signum() == 0
                ? 0.0
                : amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalSpent, 2, RoundingMode.HALF_UP)
                        .doubleValue();
        return new CategorySpendResponse(
                String.valueOf(row.getCategoryId()),
                row.getCategoryName(),
                row.getCategoryIcon(),
                row.getCategoryColor(),
                amount.setScale(2).toPlainString(),
                percentage,
                row.getTransactionCount() == null ? 0 : row.getTransactionCount());
    }
}
