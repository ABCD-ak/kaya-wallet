package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.dto.transfer.CreateTransferRequest;
import com.kaya.dto.transfer.TransferQuoteRequest;
import com.kaya.dto.transfer.TransferQuoteResponse;
import com.kaya.dto.transfer.TransferResponse;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/quote")
    public ApiResponse<TransferQuoteResponse> quote(
            @Valid @RequestBody TransferQuoteRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transferService.quote(principal.id(), request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransferResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transferService.create(principal.id(), idempotencyKey, request));
    }

    @GetMapping("/{transferId}")
    public ApiResponse<TransferResponse> get(
            @PathVariable Long transferId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transferService.get(principal.id(), transferId));
    }
}
