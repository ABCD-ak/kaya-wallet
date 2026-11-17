package com.kaya.service;

import com.kaya.common.PageResult;
import com.kaya.dto.notification.NotificationResponse;

import java.math.BigDecimal;

public interface NotificationService {

    PageResult<NotificationResponse> list(Long userId, Boolean isRead, int page, int pageSize);

    long unreadCount(Long userId);

    NotificationResponse markRead(Long userId, Long notificationId);

    int markAllRead(Long userId);

    /** 创建一条通知 */
    void create(
            Long userId,
            String type,
            String title,
            String body,
            BigDecimal amount,
            String currencyCode,
            Long relatedTransactionId);
}
