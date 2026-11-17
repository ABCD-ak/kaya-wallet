package com.kaya.dto.user;

import com.kaya.entity.UserVerification;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record VerificationResponse(
        String id,
        String type,
        String status,
        String documentType,
        String documentNoLast4,
        OffsetDateTime verifiedAt,
        String rejectedReason,
        OffsetDateTime createdAt
) {

    public static VerificationResponse from(UserVerification verification) {
        return new VerificationResponse(
                String.valueOf(verification.getId()),
                verification.getType(),
                verification.getStatus(),
                verification.getDocumentType(),
                verification.getDocumentNoLast4(),
                toUtc(verification.getVerifiedAt()),
                verification.getRejectedReason(),
                toUtc(verification.getCreatedAt()));
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
