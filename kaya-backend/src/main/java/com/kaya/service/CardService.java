package com.kaya.service;

import com.kaya.dto.card.CardResponse;
import com.kaya.dto.card.CreateCardRequest;

import java.util.List;

public interface CardService {

    List<CardResponse> list(Long userId, Long walletId, String status);

    CardResponse create(Long userId, CreateCardRequest request);

    void remove(Long userId, Long cardId);
}
