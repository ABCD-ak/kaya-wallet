package com.kaya.service;

import com.kaya.common.PageResult;
import com.kaya.dto.recipient.CreateRecipientRequest;
import com.kaya.dto.recipient.RecipientResponse;
import com.kaya.dto.recipient.UpdateRecipientRequest;

public interface RecipientService {

    PageResult<RecipientResponse> list(Long userId, String search, boolean recent, int page, int pageSize);

    RecipientResponse create(Long userId, CreateRecipientRequest request);

    RecipientResponse update(Long userId, Long recipientId, UpdateRecipientRequest request);

    void remove(Long userId, Long recipientId);
}
