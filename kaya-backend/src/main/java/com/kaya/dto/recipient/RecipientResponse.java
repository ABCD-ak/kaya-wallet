package com.kaya.dto.recipient;

import com.kaya.entity.Recipient;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record RecipientResponse(
        String id,
        String displayName,
        String phone,
        String email,
        String accountNo,
        String bankName,
        String avatarUrl,
        boolean isFavorite,
        OffsetDateTime lastUsedAt,
        OffsetDateTime createdAt
) {

    public static RecipientResponse from(Recipient recipient) {
        return new RecipientResponse(
                String.valueOf(recipient.getId()),
                recipient.getDisplayName(),
                recipient.getPhone(),
                recipient.getEmail(),
                recipient.getAccountNo(),
                recipient.getBankName(),
                recipient.getAvatarUrl(),
                recipient.isFavorite(),
                toUtc(recipient.getLastUsedAt()),
                toUtc(recipient.getCreatedAt()));
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
