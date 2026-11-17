package com.kaya.service;

import com.kaya.dto.budget.BudgetResponse;
import com.kaya.dto.budget.CreateBudgetRequest;
import com.kaya.dto.budget.UpdateBudgetRequest;

import java.util.List;

public interface BudgetService {

    List<BudgetResponse> list(Long userId, String month);

    BudgetResponse create(Long userId, CreateBudgetRequest request);

    BudgetResponse update(Long userId, Long budgetId, UpdateBudgetRequest request);

    void remove(Long userId, Long budgetId);
}
