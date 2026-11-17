package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.wallet.WalletResponse;
import com.kaya.dto.wallet.WalletSummaryResponse;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.WalletService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping
    public ApiResponse<List<WalletResponse>> list(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(walletService.list(principal.id()));
    }

    @GetMapping("/{walletId}/summary")
    public ApiResponse<WalletSummaryResponse> summary(
            @PathVariable Long walletId,
            @RequestParam(required = false) String month,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(walletService.summary(principal.id(), walletId, month));
    }
}
