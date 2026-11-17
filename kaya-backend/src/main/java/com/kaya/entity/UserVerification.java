package com.kaya.entity;

import java.time.LocalDateTime;

public class UserVerification {

    private Long id;
    private Long userId;
    private String type;
    private String status;
    private String documentType;
    private byte[] documentNoEncrypted;
    private String documentNoLast4;
    private LocalDateTime verifiedAt;
    private String rejectedReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public byte[] getDocumentNoEncrypted() {
        return documentNoEncrypted;
    }

    public void setDocumentNoEncrypted(byte[] documentNoEncrypted) {
        this.documentNoEncrypted = documentNoEncrypted;
    }

    public String getDocumentNoLast4() {
        return documentNoLast4;
    }

    public void setDocumentNoLast4(String documentNoLast4) {
        this.documentNoLast4 = documentNoLast4;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
