package com.kaya.dto.transaction;

import com.kaya.entity.Transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record TransactionResponse(
        String id,
        String referenceNo,
        String walletId,
        String type,
        String direction,
        String status,
        String amount,
        String feeAmount,
        String currencyCode,
        CategoryRef category,
        CounterpartyResponse counterparty,
        String recipientId,
        String relatedTransactionId,
        String note,
        OffsetDateTime occurredAt,
        OffsetDateTime createdAt
) {

    public static TransactionResponse from(Transaction transaction) {
        CounterpartyResponse counterparty = transaction.getCounterpartyName() == null
                ? null
                : new CounterpartyResponse(
                        transaction.getCounterpartyUserId() == null
                                ? null
                                : String.valueOf(transaction.getCounterpartyUserId()),
                        transaction.getCounterpartyName(),
                        transaction.getCounterpartyAccountNo(),
                        null);

        return new TransactionResponse(
                String.valueOf(transaction.getId()),
                transaction.getReferenceNo(),
                String.valueOf(transaction.getWalletId()),
                transaction.getType(),
                transaction.getDirection(),
                transaction.getStatus(),
                money(transaction.getAmount()),
                money(transaction.getFeeAmount()),
                transaction.getCurrencyCode(),
                CategoryRef.from(transaction.getCategory()),
                counterparty,
                transaction.getRecipientId() == null ? null : String.valueOf(transaction.getRecipientId()),
                transaction.getRelatedTransactionId() == null
                        ? null
                        : String.valueOf(transaction.getRelatedTransactionId()),
                transaction.getNote(),
                toUtc(transaction.getOccurredAt()),
                toUtc(transaction.getCreatedAt()));
    }

    public static String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2).toPlainString();
    }

    public static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
