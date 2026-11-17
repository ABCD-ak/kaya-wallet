package com.kaya.dto.wallet;

public record WalletSummaryResponse(
        String walletId,
        String currencyCode,
        String balance,
        String month,
        String income,
        String spent
) {
}
