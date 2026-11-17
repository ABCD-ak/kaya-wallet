package com.kaya.controller;

import com.kaya.common.ApiResponse;
import com.kaya.common.PageResult;
import com.kaya.dto.transaction.CreateTransactionRequest;
import com.kaya.dto.transaction.ReceiptResponse;
import com.kaya.dto.transaction.TransactionResponse;
import com.kaya.dto.transaction.TransactionSummaryResponse;
import com.kaya.security.AuthenticatedUser;
import com.kaya.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ApiResponse<PageResult<TransactionResponse>> list(
            @RequestParam(required = false) Long walletId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transactionService.list(
                principal.id(), walletId, type, direction, categoryId, from, to, page, pageSize));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransactionResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateTransactionRequest request,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transactionService.create(principal.id(), idempotencyKey, request));
    }

    @GetMapping("/summary")
    public ApiResponse<TransactionSummaryResponse> summary(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long walletId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transactionService.summary(principal.id(), month, walletId));
    }

    @GetMapping("/{transactionId}")
    public ApiResponse<TransactionResponse> detail(
            @PathVariable Long transactionId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transactionService.get(principal.id(), transactionId));
    }

    @GetMapping("/{transactionId}/receipt")
    public ApiResponse<ReceiptResponse> receipt(
            @PathVariable Long transactionId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        return ApiResponse.ok(transactionService.receipt(principal.id(), transactionId));
    }

    @GetMapping("/{transactionId}/receipt/download")
    public ResponseEntity<byte[]> receiptPdf(
            @PathVariable Long transactionId,
            Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        byte[] pdf = transactionService.receiptPdf(principal.id(), transactionId);
        String filename = "kaya-receipt-" + transactionId + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}
