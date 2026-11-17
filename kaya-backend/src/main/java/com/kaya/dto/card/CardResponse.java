package com.kaya.dto.card;

import com.kaya.entity.WalletCard;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record CardResponse(
        String id,
        String walletId,
        String cardType,
        String brand,
        String last4,
        String holderName,
        Integer expiryMonth,
        Integer expiryYear,
        String currencyCode,
        String balance,
        String creditLimit,
        boolean isPrimary,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static CardResponse from(WalletCard card) {
        return new CardResponse(
                String.valueOf(card.getId()),
                card.getWalletId() == null ? null : String.valueOf(card.getWalletId()),
                card.getCardType(),
                card.getBrand(),
                card.getCardNumberLast4(),
                card.getHolderName(),
                card.getExpiryMonth(),
                card.getExpiryYear(),
                card.getCurrencyCode(),
                money(card.getBalance()),
                card.getCreditLimit() == null ? null : money(card.getCreditLimit()),
                card.isPrimaryCard(),
                card.getStatus(),
                toUtc(card.getCreatedAt()),
                toUtc(card.getUpdatedAt()));
    }

    private static String money(BigDecimal value) {
        return value == null ? null : value.setScale(2).toPlainString();
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
