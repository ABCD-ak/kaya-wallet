package com.kaya.dto.user;

import com.kaya.entity.UserSettings;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record UserSettingsResponse(
        String locale,
        String timezone,
        String defaultCurrencyCode,
        boolean pushEnabled,
        boolean emailEnabled,
        boolean smsEnabled,
        boolean marketingEnabled,
        OffsetDateTime updatedAt
) {

    public static UserSettingsResponse from(UserSettings settings) {
        return new UserSettingsResponse(
                settings.getLocale(),
                settings.getTimezone(),
                settings.getDefaultCurrencyCode(),
                settings.isPushEnabled(),
                settings.isEmailEnabled(),
                settings.isSmsEnabled(),
                settings.isMarketingEnabled(),
                settings.getUpdatedAt() == null
                        ? null
                        : settings.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
}
