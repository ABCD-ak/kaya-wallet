package com.kaya.dto.user;

import com.kaya.entity.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record UserResponse(
        String id,
        String email,
        String phone,
        String displayName,
        String username,
        String avatarUrl,
        String gender,
        LocalDate birthday,
        boolean emailVerified,
        boolean phoneVerified,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                String.valueOf(user.getId()),
                user.getEmail(),
                user.getPhone(),
                user.getDisplayName(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getGender(),
                user.getBirthday(),
                user.getEmailVerifiedAt() != null,
                user.getPhoneVerifiedAt() != null,
                user.getStatus(),
                toUtc(user.getCreatedAt()),
                toUtc(user.getUpdatedAt()));
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
