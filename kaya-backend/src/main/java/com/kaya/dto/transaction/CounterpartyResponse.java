package com.kaya.dto.transaction;

public record CounterpartyResponse(
        String userId,
        String name,
        String accountNo,
        String bankName
) {
}
