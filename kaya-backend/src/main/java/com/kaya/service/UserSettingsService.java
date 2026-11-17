package com.kaya.service;

import com.kaya.dto.user.UpdateSettingsRequest;
import com.kaya.dto.user.UserSettingsResponse;

public interface UserSettingsService {

    UserSettingsResponse get(Long userId);

    UserSettingsResponse update(Long userId, UpdateSettingsRequest request);
}
