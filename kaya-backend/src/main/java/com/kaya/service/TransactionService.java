package com.kaya.service;

import com.kaya.common.PageResult;
import com.kaya.dto.transaction.CreateTransactionRequest;
import com.kaya.dto.transaction.ReceiptResponse;
import com.kaya.dto.transaction.TransactionResponse;
import com.kaya.dto.transaction.TransactionSummaryResponse;

import java.time.OffsetDateTime;

public interface TransactionService {

    PageResult<TransactionResponse> list(
            Long userId,
            Long walletId,
            String type,
            String direction,
            Long categoryId,
            OffsetDateTime from,
            OffsetDateTime to,
            int page,
            int pageSize);

    PageResult<TransactionResponse> listByWallet(
            Long userId,
            Long walletId,
            String type,
            String direction,
            String month,
            String keyword,
            int page,
            int pageSize);

    TransactionResponse create(Long userId, String idempotencyKey, CreateTransactionRequest request);

    TransactionResponse get(Long userId, Long transactionId);

    TransactionSummaryResponse summary(Long userId, String month, Long walletId);

    ReceiptResponse receipt(Long userId, Long transactionId);

    byte[] receiptPdf(Long userId, Long transactionId);
}
