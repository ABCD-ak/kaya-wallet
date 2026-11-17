package com.kaya.mapper;

import com.kaya.entity.UserSettings;
import org.apache.ibatis.annotations.Param;

public interface UserSettingsMapper {

    int insertDefault(@Param("userId") Long userId, @Param("currencyCode") String currencyCode);

    UserSettings findByUserId(@Param("userId") Long userId);

    int update(
            @Param("userId") Long userId,
            @Param("locale") String locale,
            @Param("timezone") String timezone,
            @Param("defaultCurrencyCode") String defaultCurrencyCode,
            @Param("pushEnabled") Boolean pushEnabled,
            @Param("emailEnabled") Boolean emailEnabled,
            @Param("smsEnabled") Boolean smsEnabled,
            @Param("marketingEnabled") Boolean marketingEnabled);
}
