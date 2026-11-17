package com.kaya.dto.transaction;

import java.util.List;

public record TransactionSummaryResponse(
        String month,
        String walletId,
        String currencyCode,
        String totalIncome,
        String totalSpent,
        String net,
        List<CategorySpendResponse> byCategory
) {
}
