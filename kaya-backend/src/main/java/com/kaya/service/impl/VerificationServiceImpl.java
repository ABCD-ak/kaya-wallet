package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.user.CreateVerificationRequest;
import com.kaya.dto.user.VerificationResponse;
import com.kaya.entity.UserVerification;
import com.kaya.mapper.UserVerificationMapper;
import com.kaya.security.FieldEncryptor;
import com.kaya.service.VerificationService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VerificationServiceImpl implements VerificationService {

    private final UserVerificationMapper verificationMapper;
    private final FieldEncryptor fieldEncryptor;

    public VerificationServiceImpl(
            UserVerificationMapper verificationMapper,
            FieldEncryptor fieldEncryptor) {
        this.verificationMapper = verificationMapper;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VerificationResponse> list(Long userId) {
        return verificationMapper.findByUserId(userId).stream()
                .map(VerificationResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public VerificationResponse submit(Long userId, CreateVerificationRequest request) {
        String type = request.type();
        String documentNo = trimToNull(request.documentNo());
        String documentType = trimToNull(request.documentType());

        if ("identity".equals(type) && documentNo == null) {
            throw new BusinessException(40001, "请填写证件号");
        }

        UserVerification verification = new UserVerification();
        verification.setUserId(userId);
        verification.setType(type);
        verification.setStatus("pending");
        verification.setDocumentType(documentType);
        if (documentNo != null) {
            verification.setDocumentNoEncrypted(fieldEncryptor.encrypt(documentNo));
            verification.setDocumentNoLast4(lastFour(documentNo));
        }

        try {
            verificationMapper.insert(verification);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40903, "该类型的认证已提交");
        }

        UserVerification saved = verificationMapper.findByUserAndType(userId, type);
        return VerificationResponse.from(saved);
    }

    private String lastFour(String value) {
        return value.length() <= 4 ? value : value.substring(value.length() - 4);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
