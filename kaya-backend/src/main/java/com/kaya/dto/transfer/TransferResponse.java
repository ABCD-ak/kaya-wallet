package com.kaya.dto.transfer;

import com.kaya.dto.transaction.CounterpartyResponse;
import com.kaya.dto.transaction.ReceiptResponse;

import java.time.OffsetDateTime;

public record TransferResponse(
        String id,
        String referenceNo,
        String walletId,
        String status,
        String amount,
        String feeAmount,
        String totalAmount,
        String currencyCode,
        CounterpartyResponse recipient,
        String note,
        OffsetDateTime occurredAt,
        String relatedTransactionId,
        ReceiptResponse receipt
) {
}
