package com.kaya.mapper;

import com.kaya.dto.wallet.MonthlyTotals;
import com.kaya.entity.Wallet;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface WalletMapper {

    int insertDefault(@Param("userId") Long userId, @Param("currencyCode") String currencyCode);

    List<Wallet> findByUserId(@Param("userId") Long userId);

    Wallet findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** Locks the wallet row so concurrent writes cannot oversell the balance. */
    Wallet findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);

    MonthlyTotals sumMonthly(
            @Param("walletId") Long walletId,
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    int updateBalance(
            @Param("id") Long id,
            @Param("balance") BigDecimal balance,
            @Param("expectedVersion") int expectedVersion);
}
