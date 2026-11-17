package com.kaya.mapper;

import com.kaya.entity.UserSession;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface UserSessionMapper {

    int insert(UserSession session);

    UserSession findByTokenHash(@Param("hash") String hash);

    int revokeById(@Param("id") Long id, @Param("revokedAt") LocalDateTime revokedAt);

    int revokeByTokenHash(@Param("hash") String hash, @Param("revokedAt") LocalDateTime revokedAt);

    int revokeAllExcept(
            @Param("userId") Long userId,
            @Param("exceptSessionId") Long exceptSessionId,
            @Param("revokedAt") LocalDateTime revokedAt);

    int touch(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
