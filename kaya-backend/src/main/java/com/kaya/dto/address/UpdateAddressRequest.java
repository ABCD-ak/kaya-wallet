package com.kaya.dto.address;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(
        @Size(max = 32, message = "地址标签过长")
        String label,

        @Size(max = 64, message = "收件人过长")
        String recipientName,

        @Size(max = 32, message = "联系电话过长")
        String phone,

        @Pattern(regexp = "^[A-Z]{2}$", message = "国家代码需为 2 位大写字母")
        String countryCode,

        @Size(max = 64, message = "省份过长")
        String province,

        @Size(max = 64, message = "城市过长")
        String city,

        @Size(max = 64, message = "区县过长")
        String district,

        @Size(max = 255, message = "详细地址过长")
        String detailAddress,

        @Size(max = 16, message = "邮编过长")
        String postalCode,

        Boolean isDefault
) {
}
