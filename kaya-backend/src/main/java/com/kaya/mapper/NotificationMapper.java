package com.kaya.mapper;

import com.kaya.entity.Notification;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationMapper {

    int insert(Notification notification);

    List<Notification> findByUserId(
            @Param("userId") Long userId,
            @Param("isRead") Boolean isRead);

    Notification findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countUnread(@Param("userId") Long userId);

    int markRead(@Param("id") Long id, @Param("userId") Long userId,
                 @Param("readAt") LocalDateTime readAt);

    int markAllRead(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);
}
