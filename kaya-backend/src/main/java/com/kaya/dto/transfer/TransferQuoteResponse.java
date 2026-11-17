package com.kaya.dto.transfer;

public record TransferQuoteResponse(
        String walletId,
        String currencyCode,
        String amount,
        String feeRate,
        String feeAmount,
        String totalAmount,
        String availableBalance,
        boolean sufficientBalance
) {
}
