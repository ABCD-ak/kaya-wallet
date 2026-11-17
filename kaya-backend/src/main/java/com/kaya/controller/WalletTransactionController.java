package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.common.PageResult;
import com.kaya.dto.transaction.TransactionResponse;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.TransactionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallets/{walletId}/transactions")
public class WalletTransactionController {

    private final TransactionService transactionService;

    public WalletTransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ApiResponse<PageResult<TransactionResponse>> list(
            @PathVariable Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transactionService.listByWallet(
                principal.id(), walletId, type, direction, month, q, page, pageSize));
    }
}
