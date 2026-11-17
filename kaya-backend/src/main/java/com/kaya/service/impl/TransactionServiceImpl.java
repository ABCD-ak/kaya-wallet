package com.kaya.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.kaya.common.BusinessException;
import com.kaya.common.PageResult;
import com.kaya.dto.transaction.CategorySpendResponse;
import com.kaya.dto.transaction.CounterpartyResponse;
import com.kaya.dto.transaction.CreateTransactionRequest;
import com.kaya.dto.transaction.ReceiptResponse;
import com.kaya.dto.transaction.TransactionResponse;
import com.kaya.dto.transaction.TransactionSummaryResponse;
import com.kaya.dto.wallet.MonthlyTotals;
import com.kaya.entity.Budget;
import com.kaya.entity.Category;
import com.kaya.entity.Transaction;
import com.kaya.entity.Wallet;
import com.kaya.mapper.BudgetMapper;
import com.kaya.mapper.CategoryMapper;
import com.kaya.mapper.TransactionMapper;
import com.kaya.mapper.WalletMapper;
import com.kaya.service.NotificationService;
import com.kaya.service.TransactionService;
import com.kaya.util.SimplePdfWriter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999999.99");
    private static final Set<String> TYPES =
            Set.of("income", "expense", "transfer_out", "transfer_in", "refund");
    private static final Set<String> DIRECTIONS = Set.of("credit", "debit");

    private final TransactionMapper transactionMapper;
    private final WalletMapper walletMapper;
    private final CategoryMapper categoryMapper;
    private final BudgetMapper budgetMapper;
    private final NotificationService notificationService;

    public TransactionServiceImpl(
            TransactionMapper transactionMapper,
            WalletMapper walletMapper,
            CategoryMapper categoryMapper,
            BudgetMapper budgetMapper,
            NotificationService notificationService) {
        this.transactionMapper = transactionMapper;
        this.walletMapper = walletMapper;
        this.categoryMapper = categoryMapper;
        this.budgetMapper = budgetMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TransactionResponse> list(
            Long userId,
            Long walletId,
            String type,
            String direction,
            Long categoryId,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int pageSize) {
        if (walletId != null) {
            requireWallet(userId, walletId);
        }
        return page(
                userId,
                walletId,
                normalizeEnum(type, TYPES, "交易类型不正确"),
                normalizeEnum(direction, DIRECTIONS, "资金方向不正确"),
                categoryId,
                toUtc(from),
                toUtc(to),
                null,
                page,
                pageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<TransactionResponse> listByWallet(
            Long userId,
            Long walletId,
            String type,
            String direction,
            String month,
            String keyword,
            int page,
            int pageSize) {
        requireWallet(userId, walletId);
        LocalDateTime from = null;
        LocalDateTime to = null;
        if (month != null && !month.isBlank()) {
            YearMonth yearMonth = parseMonth(month);
            from = yearMonth.atDay(1).atStartOfDay();
            to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        }
        String q = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return page(
                userId,
                walletId,
                normalizeEnum(type, TYPES, "交易类型不正确"),
                normalizeEnum(direction, DIRECTIONS, "资金方向不正确"),
                null,
                from,
                to,
                q,
                page,
                pageSize);
    }

    @Override
    @Transactional
    public TransactionResponse create(
            Long userId,
            String idempotencyKey,
            CreateTransactionRequest request) {
        String key = requireIdempotencyKey(idempotencyKey);

        Transaction existing = transactionMapper.findByUserIdAndIdempotencyKey(userId, key);
        if (existing != null) {
            return TransactionResponse.from(existing);
        }

        Long walletId = parseId(request.walletId(), "钱包 ID 不正确");
        Long categoryId = parseId(request.categoryId(), "分类 ID 不正确");
        BigDecimal amount = parseAmount(request.amount());

        Wallet wallet = walletMapper.findByIdAndUserId(walletId, userId);
        if (wallet == null) {
            throw new BusinessException(40401, "钱包不存在");
        }
        if (!"active".equals(wallet.getStatus())) {
            throw new BusinessException(40004, "钱包当前不可用");
        }

        Category category = categoryMapper.findById(categoryId);
        if (category == null || !Boolean.TRUE.equals(category.getIsActive())) {
            throw new BusinessException(40401, "分类不存在");
        }
        if (!request.type().equals(category.getType())) {
            throw new BusinessException(40001, "分类与收支类型不匹配");
        }

        // 记一笔仅记账，不更新钱包/银行卡余额（转账由 TransferService 处理）
        boolean income = "income".equals(request.type());

        Transaction transaction = new Transaction();
        transaction.setReferenceNo(newReferenceNo());
        transaction.setUserId(userId);
        transaction.setWalletId(walletId);
        transaction.setType(request.type());
        transaction.setDirection(income ? "credit" : "debit");
        transaction.setStatus("success");
        transaction.setAmount(amount);
        transaction.setFeeAmount(BigDecimal.ZERO);
        transaction.setCurrencyCode(wallet.getCurrencyCode());
        transaction.setCategoryId(categoryId);
        transaction.setCategory(category);
        transaction.setIdempotencyKey(key);
        transaction.setNote(trimToNull(request.note()));
        transaction.setOccurredAt(parseOccurredAt(request.occurredAt()));

        try {
            transactionMapper.insert(transaction);
        } catch (DuplicateKeyException ex) {
            Transaction duplicate = transactionMapper.findByUserIdAndIdempotencyKey(userId, key);
            if (duplicate != null) {
                return TransactionResponse.from(duplicate);
            }
            throw ex;
        }

        // 支出记账仅记账、不扣余额；但若该分类本月预算已达提醒阈值，生成“支出提醒”通知
        if (!income) {
            maybeNotifyBudgetAlert(userId, category, amount, transaction);
        }

        return TransactionResponse.from(requireTransaction(userId, transaction.getId()));
    }

    /** 预算超支/临近阈值提醒（跨过阈值时通知一次） */
    private void maybeNotifyBudgetAlert(
            Long userId, Category category, BigDecimal amount, Transaction transaction) {
        LocalDateTime occurredAt = transaction.getOccurredAt() == null
                ? LocalDateTime.now(ZoneOffset.UTC)
                : transaction.getOccurredAt();
        LocalDate periodStart = occurredAt.toLocalDate().withDayOfMonth(1);
        Budget budget = budgetMapper.findByCategoryAndMonth(userId, category.getId(), periodStart);
        if (budget == null || budget.getAmountLimit() == null) {
            return;
        }

        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        BigDecimal spent = budgetMapper.sumSpent(userId, category.getId(), periodStart, periodEnd);
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        BigDecimal threshold = budget.getAlertThreshold() == null
                ? new BigDecimal("80.00")
                : budget.getAlertThreshold();
        BigDecimal alertAt = budget.getAmountLimit()
                .multiply(threshold)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal spentBefore = spent.subtract(amount);
        if (spent.compareTo(alertAt) >= 0 && spentBefore.compareTo(alertAt) < 0) {
            String symbol = currencySymbol(transaction.getCurrencyCode());
            boolean exceeded = spent.compareTo(budget.getAmountLimit()) >= 0;
            String title = exceeded ? "预算已超支" : "支出提醒";
            String body = "「" + category.getName() + "」本月已支出 "
                    + symbol + spent.setScale(2, RoundingMode.HALF_UP).toPlainString()
                    + "，预算 " + symbol
                    + budget.getAmountLimit().setScale(2, RoundingMode.HALF_UP).toPlainString();
            notificationService.create(
                    userId, "budget_alert", title, body, amount,
                    transaction.getCurrencyCode(), transaction.getId());
        }
    }

    private String currencySymbol(String code) {
        if (code == null) {
            return "¥";
        }
        return switch (code) {
            case "USD" -> "$";
            case "EUR" -> "€";
            case "JPY" -> "¥";
            case "GBP" -> "£";
            case "HKD" -> "HK$";
            default -> "¥";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse get(Long userId, Long transactionId) {
        return TransactionResponse.from(requireTransaction(userId, transactionId));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSummaryResponse summary(Long userId, String monthParam, Long walletId) {
        YearMonth month = parseMonth(monthParam);
        LocalDateTime start = month.atDay(1).atStartOfDay();
        LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();

        String currencyCode = "CNY";
        if (walletId != null) {
            currencyCode = requireWallet(userId, walletId).getCurrencyCode();
        } else {
            List<Wallet> wallets = walletMapper.findByUserId(userId);
            if (!wallets.isEmpty()) {
                currencyCode = wallets.get(0).getCurrencyCode();
            }
        }

        MonthlyTotals totals = transactionMapper.sumMonthly(userId, walletId, start, end);
        BigDecimal income = totals == null || totals.getIncome() == null ? BigDecimal.ZERO : totals.getIncome();
        BigDecimal spent = totals == null || totals.getSpent() == null ? BigDecimal.ZERO : totals.getSpent();

        List<CategorySpendResponse> byCategory = transactionMapper
                .sumExpenseByCategory(userId, walletId, start, end).stream()
                .map(row -> CategorySpendResponse.from(row, spent))
                .toList();

        return new TransactionSummaryResponse(
                month.toString(),
                walletId == null ? null : String.valueOf(walletId),
                currencyCode,
                income.setScale(2).toPlainString(),
                spent.setScale(2).toPlainString(),
                income.subtract(spent).setScale(2).toPlainString(),
                byCategory);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse receipt(Long userId, Long transactionId) {
        Transaction transaction = requireTransaction(userId, transactionId);
        BigDecimal amount = transaction.getAmount();
        BigDecimal fee = transaction.getFeeAmount() == null ? BigDecimal.ZERO : transaction.getFeeAmount();

        CounterpartyResponse recipient = transaction.getCounterpartyName() == null
                ? null
                : new CounterpartyResponse(
                        transaction.getCounterpartyUserId() == null
                                ? null
                                : String.valueOf(transaction.getCounterpartyUserId()),
                        transaction.getCounterpartyName(),
                        transaction.getCounterpartyAccountNo(),
                        null);

        return new ReceiptResponse(
                String.valueOf(transaction.getId()),
                transaction.getReferenceNo(),
                TransactionResponse.toUtc(transaction.getOccurredAt()),
                transaction.getType(),
                transaction.getStatus(),
                TransactionResponse.money(amount),
                TransactionResponse.money(fee),
                TransactionResponse.money(amount.add(fee)),
                transaction.getCurrencyCode(),
                recipient,
                transaction.getNote(),
                OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] receiptPdf(Long userId, Long transactionId) {
        ReceiptResponse receipt = receipt(userId, transactionId);
        String currency = receipt.currencyCode();
        String recipient = receipt.recipient() == null ? "-" : receipt.recipient().name();
        String account = receipt.recipient() == null || receipt.recipient().accountNo() == null
                ? "-"
                : receipt.recipient().accountNo();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.of("Asia/Shanghai"));
        String type = receipt.transactionType();
        boolean transfer = "transfer_out".equals(type) || "transfer_in".equals(type);
        String title = transfer
                ? "Kaya 转账回执"
                : ("refund".equals(type) ? "Kaya 退款回执" : "Kaya 交易回执");
        String amountLabel = transfer
                ? "转账金额"
                : ("refund".equals(type) ? "退款金额" : "交易金额");
        String partyLabel = "transfer_in".equals(type) ? "付款人" : (transfer ? "收款人" : "交易对方");
        String accountLabel = "transfer_in".equals(type) ? "付款账号" : (transfer ? "收款账号" : "对方账号");

        List<String> lines = List.of(
                "交易单号：" + receipt.referenceNo(),
                "交易时间：" + formatter.format(receipt.occurredAt()),
                "交易类型：" + typeLabel(type),
                "交易状态：" + statusLabel(receipt.status()),
                amountLabel + "：" + currency + " " + receipt.amount(),
                "手续费：" + currency + " " + receipt.feeAmount(),
                "合计金额：" + currency + " " + receipt.totalAmount(),
                partyLabel + "：" + recipient,
                accountLabel + "：" + account,
                "备注：" + (receipt.note() == null ? "-" : receipt.note()),
                "生成时间：" + formatter.format(receipt.generatedAt()));
        return SimplePdfWriter.singlePage(title, lines);
    }

    private static String typeLabel(String type) {
        if (type == null) return "-";
        return switch (type) {
            case "income" -> "收入";
            case "expense" -> "支出";
            case "transfer_out" -> "转出";
            case "transfer_in" -> "转入";
            case "refund" -> "退款";
            default -> type;
        };
    }

    private static String statusLabel(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "success" -> "成功";
            case "pending" -> "处理中";
            case "failed" -> "失败";
            default -> status;
        };
    }

    private PageResult<TransactionResponse> page(
            Long userId,
            Long walletId,
            String type,
            String direction,
            Long categoryId,
            LocalDateTime from,
            LocalDateTime to,
            String keyword,
            int page,
            int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        PageHelper.startPage(safePage, safePageSize);
        List<Transaction> transactions = transactionMapper.search(
                userId, walletId, type, direction, categoryId, from, to, keyword);
        PageInfo<Transaction> pageInfo = new PageInfo<>(transactions);

        return new PageResult<>(
                pageInfo.getList().stream().map(TransactionResponse::from).toList(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getPages());
    }

    private Transaction requireTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionMapper.findByIdAndUserId(transactionId, userId);
        if (transaction == null) {
            throw new BusinessException(40401, "交易不存在");
        }
        return transaction;
    }

    private Wallet requireWallet(Long userId, Long walletId) {
        Wallet wallet = walletMapper.findByIdAndUserId(walletId, userId);
        if (wallet == null) {
            throw new BusinessException(40401, "钱包不存在");
        }
        return wallet;
    }

    private String normalizeEnum(String value, Set<String> allowed, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (!allowed.contains(normalized)) {
            throw new BusinessException(40001, message);
        }
        return normalized;
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

    private String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(40001, "缺少 Idempotency-Key 请求头");
        }
        String key = value.trim();
        if (key.length() < 8 || key.length() > 64) {
            throw new BusinessException(40001, "Idempotency-Key 长度需为 8-64 位");
        }
        return key;
    }

    private Long parseId(String value, String message) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException(40001, message);
        }
    }

    private BigDecimal parseAmount(String value) {
        try {
            BigDecimal amount = new BigDecimal(value);
            if (amount.signum() <= 0 || amount.scale() > 2 || amount.compareTo(MAX_AMOUNT) > 0) {
                throw new BusinessException(40001, "金额需为两位小数的正数");
            }
            return amount.setScale(2);
        } catch (NumberFormatException ex) {
            throw new BusinessException(40001, "金额格式不正确");
        }
    }

    private LocalDateTime parseOccurredAt(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now(ZoneOffset.UTC);
        }
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException e2) {
                return LocalDateTime.now(ZoneOffset.UTC);
            }
        }
    }

    private LocalDateTime toUtc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private String newReferenceNo() {
        return "TX-" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 20)
                .toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
