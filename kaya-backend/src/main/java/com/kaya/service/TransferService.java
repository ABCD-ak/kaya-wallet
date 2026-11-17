package com.kaya.service;

import com.kaya.dto.transfer.CreateTransferRequest;
import com.kaya.dto.transfer.TransferQuoteRequest;
import com.kaya.dto.transfer.TransferQuoteResponse;
import com.kaya.dto.transfer.TransferResponse;

public interface TransferService {

    TransferQuoteResponse quote(Long userId, TransferQuoteRequest request);

    TransferResponse create(Long userId, String idempotencyKey, CreateTransferRequest request);

    TransferResponse get(Long userId, Long transferId);
}
