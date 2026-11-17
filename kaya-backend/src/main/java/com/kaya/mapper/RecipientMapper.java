package com.kaya.mapper;

import com.kaya.entity.Recipient;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface RecipientMapper {

    List<Recipient> search(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("recent") boolean recent);

    Recipient findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Recipient findByUserIdAndAccountNo(
            @Param("userId") Long userId,
            @Param("accountNo") String accountNo);

    int insert(Recipient recipient);

    int update(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("displayName") String displayName,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("accountNo") String accountNo,
            @Param("bankName") String bankName,
            @Param("avatarUrl") String avatarUrl,
            @Param("favorite") Boolean favorite);

    int markDeleted(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("deletedAt") LocalDateTime deletedAt);

    int touchLastUsed(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);
}
