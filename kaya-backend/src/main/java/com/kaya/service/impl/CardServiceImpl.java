package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.card.CardResponse;
import com.kaya.dto.card.CreateCardRequest;
import com.kaya.entity.WalletCard;
import com.kaya.mapper.WalletCardMapper;
import com.kaya.service.CardService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class CardServiceImpl implements CardService {

    private static final List<String> STATUSES = List.of("active", "expired", "frozen", "removed");

    private final WalletCardMapper cardMapper;

    public CardServiceImpl(WalletCardMapper cardMapper) {
        this.cardMapper = cardMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponse> list(Long userId, Long walletId, String status) {
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim();
        if (normalizedStatus != null && !STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(40001, "卡状态不正确");
        }
        return cardMapper.findByUserId(userId, walletId, normalizedStatus).stream()
                .map(CardResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public CardResponse create(Long userId, CreateCardRequest request) {
        WalletCard card = new WalletCard();
        card.setUserId(userId);
        card.setCardType(request.cardType());
        card.setBrand(request.brand());
        card.setCardNumberLast4(request.last4());
        card.setHolderName(request.holderName().trim());
        card.setExpiryMonth(request.expiryMonth());
        card.setExpiryYear(request.expiryYear());
        card.setCurrencyCode(request.currencyCode());
        card.setBalance(BigDecimal.ZERO);
        card.setPaymentToken(trimToNull(request.paymentToken()));
        card.setPrimaryCard(Boolean.TRUE.equals(request.isPrimary()));
        card.setStatus("active");

        try {
            cardMapper.insert(card);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40904, "该银行卡已绑定");
        }

        if (card.isPrimaryCard()) {
            cardMapper.clearPrimary(userId, card.getId());
        }
        return CardResponse.from(cardMapper.findByIdAndUserId(card.getId(), userId));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long cardId) {
        int updated = cardMapper.markRemoved(cardId, userId, LocalDateTime.now(ZoneOffset.UTC));
        if (updated == 0) {
            throw new BusinessException(40401, "银行卡不存在");
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
