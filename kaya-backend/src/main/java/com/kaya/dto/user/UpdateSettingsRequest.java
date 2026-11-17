package com.kaya.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
        @Size(max = 16, message = "语言代码过长")
        @Pattern(regexp = "^[a-zA-Z]{2}(-[a-zA-Z]{2,4})?$", message = "语言代码格式不正确")
        String locale,

        @Size(max = 64, message = "时区名称过长")
        String timezone,

        @Pattern(regexp = "^[A-Z]{3}$", message = "币种需为 3 位大写字母")
        String defaultCurrencyCode,

        Boolean pushEnabled,
        Boolean emailEnabled,
        Boolean smsEnabled,
        Boolean marketingEnabled
) {
}
