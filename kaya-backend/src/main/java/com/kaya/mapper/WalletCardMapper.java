package com.kaya.mapper;

import com.kaya.entity.WalletCard;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WalletCardMapper {

    List<WalletCard> findByUserId(
            @Param("userId") Long userId,
            @Param("walletId") Long walletId,
            @Param("status") String status);

    WalletCard findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int insert(WalletCard card);

    int clearPrimary(@Param("userId") Long userId, @Param("exceptId") Long exceptId);

    int markRemoved(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("removedAt") LocalDateTime removedAt);

    int deductBalance(@Param("id") Long id, @Param("userId") Long userId,
                      @Param("amount") BigDecimal amount);
}
