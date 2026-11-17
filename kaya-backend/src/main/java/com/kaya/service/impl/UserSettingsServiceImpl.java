package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.user.UpdateSettingsRequest;
import com.kaya.dto.user.UserSettingsResponse;
import com.kaya.entity.UserSettings;
import com.kaya.mapper.UserSettingsMapper;
import com.kaya.service.UserSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;

@Service
public class UserSettingsServiceImpl implements UserSettingsService {

    private static final String DEFAULT_CURRENCY = "CNY";

    private final UserSettingsMapper userSettingsMapper;

    public UserSettingsServiceImpl(UserSettingsMapper userSettingsMapper) {
        this.userSettingsMapper = userSettingsMapper;
    }

    @Override
    @Transactional
    public UserSettingsResponse get(Long userId) {
        UserSettings settings = userSettingsMapper.findByUserId(userId);
        if (settings == null) {
            userSettingsMapper.insertDefault(userId, DEFAULT_CURRENCY);
            settings = userSettingsMapper.findByUserId(userId);
        }
        return UserSettingsResponse.from(settings);
    }

    @Override
    @Transactional
    public UserSettingsResponse update(Long userId, UpdateSettingsRequest request) {
        String locale = trimToNull(request.locale());
        String timezone = trimToNull(request.timezone());
        String currency = trimToNull(request.defaultCurrencyCode());

        if (locale != null && !locale.matches("^[a-zA-Z]{2}(-[a-zA-Z]{2,4})?$")) {
            throw new BusinessException(40001, "语言代码格式不正确");
        }
        if (timezone != null) {
            try {
                ZoneId.of(timezone);
            } catch (DateTimeException ex) {
                throw new BusinessException(40001, "时区名称不正确");
            }
        }
        if (currency != null && !currency.matches("^[A-Z]{3}$")) {
            throw new BusinessException(40001, "币种需为 3 位大写字母");
        }

        boolean hasChange = locale != null || timezone != null || currency != null
                || request.pushEnabled() != null || request.emailEnabled() != null
                || request.smsEnabled() != null || request.marketingEnabled() != null;

        if (hasChange) {
            userSettingsMapper.update(
                    userId,
                    locale,
                    timezone,
                    currency,
                    request.pushEnabled(),
                    request.emailEnabled(),
                    request.smsEnabled(),
                    request.marketingEnabled());
        }

        UserSettings settings = userSettingsMapper.findByUserId(userId);
        if (settings == null) {
            throw new BusinessException(40401, "偏好设置不存在");
        }
        return UserSettingsResponse.from(settings);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
