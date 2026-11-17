package com.kaya.mapper;

import com.kaya.entity.Budget;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BudgetMapper {

    List<Budget> search(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    Budget findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Budget findByCategoryAndMonth(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("periodStart") LocalDate periodStart);

    int insert(Budget budget);

    int update(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("amountLimit") BigDecimal amountLimit,
            @Param("alertThreshold") BigDecimal alertThreshold);

    int delete(@Param("id") Long id, @Param("userId") Long userId);

    BigDecimal sumSpent(
            @Param("userId") Long userId,
            @Param("categoryId") Long categoryId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
