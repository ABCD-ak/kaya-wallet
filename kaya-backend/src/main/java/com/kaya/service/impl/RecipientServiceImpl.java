package com.kaya.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.kaya.common.BusinessException;
import com.kaya.common.PageResult;
import com.kaya.dto.recipient.CreateRecipientRequest;
import com.kaya.dto.recipient.RecipientResponse;
import com.kaya.dto.recipient.UpdateRecipientRequest;
import com.kaya.entity.Recipient;
import com.kaya.mapper.RecipientMapper;
import com.kaya.service.RecipientService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class RecipientServiceImpl implements RecipientService {

    private static final int MAX_PAGE_SIZE = 100;

    private final RecipientMapper recipientMapper;

    public RecipientServiceImpl(RecipientMapper recipientMapper) {
        this.recipientMapper = recipientMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<RecipientResponse> list(
            Long userId, String search, boolean recent, int page, int pageSize) {
        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String keyword = search == null || search.isBlank() ? null : search.trim();

        PageHelper.startPage(safePage, safePageSize);
        List<Recipient> recipients = recipientMapper.search(userId, keyword, recent);
        PageInfo<Recipient> pageInfo = new PageInfo<>(recipients);

        return new PageResult<>(
                pageInfo.getList().stream().map(RecipientResponse::from).toList(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize(),
                pageInfo.getTotal(),
                pageInfo.getPages());
    }

    @Override
    @Transactional
    public RecipientResponse create(Long userId, CreateRecipientRequest request) {
        Recipient recipient = new Recipient();
        recipient.setUserId(userId);
        recipient.setDisplayName(request.displayName().trim());
        recipient.setPhone(trimToNull(request.phone()));
        recipient.setEmail(trimToNull(request.email()));
        recipient.setAccountNo(trimToNull(request.accountNo()));
        recipient.setBankName(trimToNull(request.bankName()));
        recipient.setAvatarUrl(trimToNull(request.avatarUrl()));
        recipient.setFavorite(Boolean.TRUE.equals(request.isFavorite()));

        try {
            recipientMapper.insert(recipient);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40904, "该收款账号已存在");
        }
        return RecipientResponse.from(requireRecipient(userId, recipient.getId()));
    }

    @Override
    @Transactional
    public RecipientResponse update(Long userId, Long recipientId, UpdateRecipientRequest request) {
        requireRecipient(userId, recipientId);

        String displayName = request.displayName();
        if (displayName != null) {
            displayName = displayName.trim();
            if (displayName.isEmpty()) {
                throw new BusinessException(40001, "收款人姓名不能为空");
            }
        }

        try {
            recipientMapper.update(
                    recipientId,
                    userId,
                    displayName,
                    trimToNull(request.phone()),
                    trimToNull(request.email()),
                    trimToNull(request.accountNo()),
                    trimToNull(request.bankName()),
                    trimToNull(request.avatarUrl()),
                    request.isFavorite());
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40904, "该收款账号已存在");
        }
        return RecipientResponse.from(requireRecipient(userId, recipientId));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long recipientId) {
        if (recipientMapper.markDeleted(recipientId, userId, LocalDateTime.now(ZoneOffset.UTC)) == 0) {
            throw new BusinessException(40401, "联系人不存在");
        }
    }

    private Recipient requireRecipient(Long userId, Long recipientId) {
        Recipient recipient = recipientMapper.findByIdAndUserId(recipientId, userId);
        if (recipient == null) {
            throw new BusinessException(40401, "联系人不存在");
        }
        return recipient;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
