package com.kaya.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.kaya.common.BusinessException;
import com.kaya.common.PageResult;
import com.kaya.dto.notification.NotificationResponse;
import com.kaya.entity.Notification;
import com.kaya.mapper.NotificationMapper;
import com.kaya.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<NotificationResponse> list(
            Long userId, Boolean isRead, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        PageHelper.startPage(safePage, safePageSize);
        List<Notification> notifications = notificationMapper.findByUserId(userId, isRead);
        PageInfo<Notification> pageInfo = new PageInfo<>(notifications);

        return new PageResult<>(
                pageInfo.getList().stream().map(NotificationResponse::from).toList(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getPages());
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long userId, Long notificationId) {
        Notification notification = require(userId, notificationId);
        if (!notification.isRead()) {
            notificationMapper.markRead(notificationId, userId, LocalDateTime.now(ZoneOffset.UTC));
            notification = require(userId, notificationId);
        }
        return NotificationResponse.from(notification);
    }

    @Override
    @Transactional
    public int markAllRead(Long userId) {
        return notificationMapper.markAllRead(userId, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Override
    @Transactional
    public void create(
            Long userId,
            String type,
            String title,
            String body,
            BigDecimal amount,
            String currencyCode,
            Long relatedTransactionId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setAmount(amount);
        notification.setCurrencyCode(currencyCode);
        notification.setRelatedTransactionId(relatedTransactionId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        notificationMapper.insert(notification);
    }

    private Notification require(Long userId, Long notificationId) {
        Notification notification = notificationMapper.findByIdAndUserId(notificationId, userId);
        if (notification == null) {
            throw new BusinessException(40401, "通知不存在");
        }
        return notification;
    }
}
