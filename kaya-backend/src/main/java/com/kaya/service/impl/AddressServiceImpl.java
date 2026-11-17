package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.address.AddressResponse;
import com.kaya.dto.address.CreateAddressRequest;
import com.kaya.dto.address.UpdateAddressRequest;
import com.kaya.entity.UserAddress;
import com.kaya.mapper.UserAddressMapper;
import com.kaya.service.AddressService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    private final UserAddressMapper addressMapper;

    public AddressServiceImpl(UserAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> list(Long userId) {
        return addressMapper.findByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse create(Long userId, CreateAddressRequest request) {
        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setLabel(trimToNull(request.label()));
        address.setRecipientName(request.recipientName().trim());
        address.setPhone(request.phone().trim());
        address.setCountryCode(request.countryCode());
        address.setProvince(trimToNull(request.province()));
        address.setCity(trimToNull(request.city()));
        address.setDistrict(trimToNull(request.district()));
        address.setDetailAddress(request.detailAddress().trim());
        address.setPostalCode(trimToNull(request.postalCode()));
        address.setDefaultAddress(Boolean.TRUE.equals(request.isDefault()));

        addressMapper.insert(address);
        if (address.isDefaultAddress()) {
            addressMapper.clearDefault(userId, address.getId());
        }
        return AddressResponse.from(requireAddress(userId, address.getId()));
    }

    @Override
    @Transactional
    public AddressResponse update(Long userId, Long addressId, UpdateAddressRequest request) {
        requireAddress(userId, addressId);

        validateNotBlank(request.recipientName(), "收件人不能为空");
        validateNotBlank(request.phone(), "联系电话不能为空");
        validateNotBlank(request.detailAddress(), "详细地址不能为空");

        addressMapper.update(
                addressId,
                userId,
                trimToNull(request.label()),
                trimToNull(request.recipientName()),
                trimToNull(request.phone()),
                trimToNull(request.countryCode()),
                trimToNull(request.province()),
                trimToNull(request.city()),
                trimToNull(request.district()),
                trimToNull(request.detailAddress()),
                trimToNull(request.postalCode()),
                request.isDefault());

        if (Boolean.TRUE.equals(request.isDefault())) {
            addressMapper.clearDefault(userId, addressId);
        }
        return AddressResponse.from(requireAddress(userId, addressId));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long addressId) {
        if (addressMapper.markDeleted(addressId, userId, LocalDateTime.now(ZoneOffset.UTC)) == 0) {
            throw new BusinessException(40401, "地址不存在");
        }
    }

    private UserAddress requireAddress(Long userId, Long addressId) {
        UserAddress address = addressMapper.findByIdAndUserId(addressId, userId);
        if (address == null) {
            throw new BusinessException(40401, "地址不存在");
        }
        return address;
    }

    private void validateNotBlank(String value, String message) {
        if (value != null && value.trim().isEmpty()) {
            throw new BusinessException(40001, message);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
