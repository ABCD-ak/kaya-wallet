package com.kaya.dto.notification;

import com.kaya.entity.Notification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record NotificationResponse(
        String id,
        String type,
        String title,
        String body,
        String amount,
        String currencyCode,
        String relatedTransactionId,
        boolean isRead,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {

    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                String.valueOf(notification.getId()),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getAmount() == null
                        ? null
                        : notification.getAmount().setScale(2).toPlainString(),
                notification.getCurrencyCode(),
                notification.getRelatedTransactionId() == null
                        ? null
                        : String.valueOf(notification.getRelatedTransactionId()),
                notification.isRead(),
                toUtc(notification.getReadAt()),
                toUtc(notification.getCreatedAt()));
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
