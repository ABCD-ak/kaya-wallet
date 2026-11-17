package com.kaya.dto.wallet;

import com.kaya.entity.Wallet;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record WalletResponse(
        String id,
        String currencyCode,
        String balance,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                String.valueOf(wallet.getId()),
                wallet.getCurrencyCode(),
                wallet.getBalance().setScale(2).toPlainString(),
                wallet.getStatus(),
                toUtc(wallet.getCreatedAt()),
                toUtc(wallet.getUpdatedAt()));
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
