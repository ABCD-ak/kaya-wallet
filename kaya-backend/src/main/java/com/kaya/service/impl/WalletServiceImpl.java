package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.wallet.MonthlyTotals;
import com.kaya.dto.wallet.WalletResponse;
import com.kaya.dto.wallet.WalletSummaryResponse;
import com.kaya.entity.Wallet;
import com.kaya.mapper.WalletMapper;
import com.kaya.service.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;

    public WalletServiceImpl(WalletMapper walletMapper) {
        this.walletMapper = walletMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> list(Long userId) {
        return walletMapper.findByUserId(userId).stream()
                .map(WalletResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryResponse summary(Long userId, Long walletId, String monthParam) {
        Wallet wallet = walletMapper.findByIdAndUserId(walletId, userId);
        if (wallet == null) {
            throw new BusinessException(40401, "钱包不存在");
        }

        YearMonth month = parseMonth(monthParam);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();

        MonthlyTotals totals = walletMapper.sumMonthly(walletId, userId, start, end);
        return new WalletSummaryResponse(
                String.valueOf(wallet.getId()),
                wallet.getCurrencyCode(),
                wallet.getBalance().setScale(2).toPlainString(),
                month.toString(),
                money(totals == null ? null : totals.getIncome()),
                money(totals == null ? null : totals.getSpent()));
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        if (!value.matches("^[0-9]{4}-[0-9]{2}$")) {
            throw new BusinessException(40001, "月份格式需为 YYYY-MM");
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(40001, "月份格式需为 YYYY-MM");
        }
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2).toPlainString();
    }
}
