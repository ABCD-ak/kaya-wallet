package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.budget.BudgetResponse;
import com.kaya.dto.budget.CreateBudgetRequest;
import com.kaya.dto.budget.UpdateBudgetRequest;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping
    public ApiResponse<List<BudgetResponse>> list(
            @RequestParam(required = false) String month,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(budgetService.list(principal.id(), month));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BudgetResponse> create(
            @Valid @RequestBody CreateBudgetRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(budgetService.create(principal.id(), request));
    }

    @PatchMapping("/{budgetId}")
    public ApiResponse<BudgetResponse> update(
            @PathVariable Long budgetId,
            @Valid @RequestBody UpdateBudgetRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(budgetService.update(principal.id(), budgetId, request));
    }

    @DeleteMapping("/{budgetId}")
    public ApiResponse<Void> remove(
            @PathVariable Long budgetId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        budgetService.remove(principal.id(), budgetId);
        return ApiResponse.ok(null);
    }
}
