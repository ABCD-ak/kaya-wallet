package com.kaya.mapper;

import com.kaya.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface PasswordResetTokenMapper {

    int insert(PasswordResetToken token);

    PasswordResetToken findValidByHash(
            @Param("hash") String hash,
            @Param("now") LocalDateTime now);

    int markUsed(@Param("id") Long id, @Param("usedAt") LocalDateTime usedAt);

    int invalidateActive(@Param("userId") Long userId, @Param("usedAt") LocalDateTime usedAt);
}
