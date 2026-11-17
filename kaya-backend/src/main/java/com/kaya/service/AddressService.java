package com.kaya.service;

import com.kaya.dto.address.AddressResponse;
import com.kaya.dto.address.CreateAddressRequest;
import com.kaya.dto.address.UpdateAddressRequest;

import java.util.List;

public interface AddressService {

    List<AddressResponse> list(Long userId);

    AddressResponse create(Long userId, CreateAddressRequest request);

    AddressResponse update(Long userId, Long addressId, UpdateAddressRequest request);

    void remove(Long userId, Long addressId);
}
