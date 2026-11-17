package com.kaya.dto.address;

import com.kaya.entity.UserAddress;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record AddressResponse(
        String id,
        String label,
        String recipientName,
        String phone,
        String countryCode,
        String province,
        String city,
        String district,
        String detailAddress,
        String postalCode,
        boolean isDefault,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static AddressResponse from(UserAddress address) {
        return new AddressResponse(
                String.valueOf(address.getId()),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhone(),
                address.getCountryCode(),
                address.getProvince(),
                address.getCity(),
                address.getDistrict(),
                address.getDetailAddress(),
                address.getPostalCode(),
                address.isDefaultAddress(),
                toUtc(address.getCreatedAt()),
                toUtc(address.getUpdatedAt()));
    }

    private static OffsetDateTime toUtc(java.time.LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
