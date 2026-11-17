package com.kaya.dto.transaction;

import java.time.OffsetDateTime;

public record ReceiptResponse(
        String transactionId,
        String referenceNo,
        OffsetDateTime occurredAt,
        String transactionType,
        String status,
        String amount,
        String feeAmount,
        String totalAmount,
        String currencyCode,
        CounterpartyResponse recipient,
        String note,
        OffsetDateTime generatedAt
) {
}
