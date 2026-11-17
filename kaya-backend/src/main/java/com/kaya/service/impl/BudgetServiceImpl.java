package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.budget.BudgetResponse;
import com.kaya.dto.budget.CreateBudgetRequest;
import com.kaya.dto.budget.UpdateBudgetRequest;
import com.kaya.entity.Budget;
import com.kaya.entity.Category;
import com.kaya.entity.UserSettings;
import com.kaya.mapper.BudgetMapper;
import com.kaya.mapper.CategoryMapper;
import com.kaya.mapper.UserSettingsMapper;
import com.kaya.service.BudgetService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class BudgetServiceImpl implements BudgetService {

    private static final String DEFAULT_CURRENCY = "CNY";
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("80.00");

    private final BudgetMapper budgetMapper;
    private final CategoryMapper categoryMapper;
    private final UserSettingsMapper userSettingsMapper;

    public BudgetServiceImpl(
            BudgetMapper budgetMapper,
            CategoryMapper categoryMapper,
            UserSettingsMapper userSettingsMapper) {
        this.budgetMapper = budgetMapper;
        this.categoryMapper = categoryMapper;
        this.userSettingsMapper = userSettingsMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId, String month) {
        YearMonth target = parseMonth(month);
        LocalDate from = target.atDay(1);
        LocalDate to = target.atEndOfMonth();
        String currency = currencyOf(userId);

        return budgetMapper.search(userId, from, to).stream()
                .map(budget -> toResponse(userId, budget, currency))
                .toList();
    }

    @Override
    @Transactional
    public BudgetResponse create(Long userId, CreateBudgetRequest request) {
        Category category = categoryMapper.findById(Long.parseLong(request.categoryId()));
        if (category == null || !Boolean.TRUE.equals(category.getIsActive())) {
            throw new BusinessException(40401, "分类不存在");
        }
        if (!"expense".equals(category.getType())) {
            throw new BusinessException(40001, "仅支出分类可设置预算");
        }

        YearMonth target = parseMonth(request.month());
        BigDecimal amountLimit = parseAmount(request.amountLimit());
        BigDecimal threshold = request.alertThreshold() == null
                ? DEFAULT_THRESHOLD
                : request.alertThreshold().setScale(2, RoundingMode.HALF_UP);

        LocalDate periodStart = target.atDay(1);
        if (budgetMapper.findByCategoryAndMonth(userId, category.getId(), periodStart) != null) {
            throw new BusinessException(40904, "该分类本月预算已存在");
        }

        Budget budget = new Budget();
        budget.setUserId(userId);
        budget.setCategoryId(category.getId());
        budget.setPeriodType("monthly");
        budget.setPeriodStart(periodStart);
        budget.setPeriodEnd(target.atEndOfMonth());
        budget.setAmountLimit(amountLimit);
        budget.setAlertThreshold(threshold);

        try {
            budgetMapper.insert(budget);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40904, "该分类本月预算已存在");
        }

        return toResponse(userId, require(userId, budget.getId()), currencyOf(userId));
    }

    @Override
    @Transactional
    public BudgetResponse update(Long userId, Long budgetId, UpdateBudgetRequest request) {
        require(userId, budgetId);

        BigDecimal amountLimit = request.amountLimit() == null
                ? null
                : parseAmount(request.amountLimit());
        BigDecimal threshold = request.alertThreshold() == null
                ? null
                : request.alertThreshold().setScale(2, RoundingMode.HALF_UP);

        budgetMapper.update(budgetId, userId, amountLimit, threshold);
        return toResponse(userId, require(userId, budgetId), currencyOf(userId));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long budgetId) {
        if (budgetMapper.delete(budgetId, userId) == 0) {
            throw new BusinessException(40401, "预算不存在");
        }
    }

    private BudgetResponse toResponse(Long userId, Budget budget, String currency) {
        Category category = budget.getCategory();
        BigDecimal spent = budgetMapper.sumSpent(
                userId, budget.getCategoryId(), budget.getPeriodStart(), budget.getPeriodEnd());

        return BudgetResponse.of(
                budget.getId(),
                budget.getCategoryId(),
                category == null ? null : category.getName(),
                category == null ? null : category.getIcon(),
                category == null ? null : category.getColor(),
                budget.getPeriodType(),
                budget.getPeriodStart(),
                budget.getPeriodEnd(),
                budget.getAmountLimit(),
                spent,
                budget.getAlertThreshold(),
                currency);
    }

    private Budget require(Long userId, Long budgetId) {
        Budget budget = budgetMapper.findByIdAndUserId(budgetId, userId);
        if (budget == null) {
            throw new BusinessException(40401, "预算不存在");
        }
        return budget;
    }

    private String currencyOf(Long userId) {
        UserSettings settings = userSettingsMapper.findByUserId(userId);
        String currency = settings == null ? null : settings.getDefaultCurrencyCode();
        return currency == null || currency.isBlank() ? DEFAULT_CURRENCY : currency;
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(ZoneOffset.UTC);
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException(40001, "月份格式需为 YYYY-MM");
        }
    }

    private BigDecimal parseAmount(String amount) {
        try {
            BigDecimal value = new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP);
            if (value.signum() <= 0) {
                throw new BusinessException(40001, "预算金额需大于 0");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new BusinessException(40001, "金额格式不正确");
        }
    }
}
