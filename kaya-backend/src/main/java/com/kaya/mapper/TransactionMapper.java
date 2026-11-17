package com.kaya.mapper;

import com.kaya.dto.transaction.CategorySpendRow;
import com.kaya.dto.wallet.MonthlyTotals;
import com.kaya.entity.Transaction;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionMapper {

    List<Transaction> search(
            @Param("userId") Long userId,
            @Param("walletId") Long walletId,
            @Param("type") String type,
            @Param("direction") String direction,
            @Param("categoryId") Long categoryId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("keyword") String keyword);

    Transaction findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Transaction findByUserIdAndIdempotencyKey(
            @Param("userId") Long userId,
            @Param("idempotencyKey") String idempotencyKey);

    int insert(Transaction transaction);

    MonthlyTotals sumMonthly(
            @Param("userId") Long userId,
            @Param("walletId") Long walletId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<CategorySpendRow> sumExpenseByCategory(
            @Param("userId") Long userId,
            @Param("walletId") Long walletId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
