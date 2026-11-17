package com.kaya.mapper;

import com.kaya.entity.UserAddress;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserAddressMapper {

    List<UserAddress> findByUserId(@Param("userId") Long userId);

    UserAddress findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int insert(UserAddress address);

    int update(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("label") String label,
            @Param("recipientName") String recipientName,
            @Param("phone") String phone,
            @Param("countryCode") String countryCode,
            @Param("province") String province,
            @Param("city") String city,
            @Param("district") String district,
            @Param("detailAddress") String detailAddress,
            @Param("postalCode") String postalCode,
            @Param("defaultAddress") Boolean defaultAddress);

    int clearDefault(@Param("userId") Long userId, @Param("exceptId") Long exceptId);

    int markDeleted(@Param("id") Long id, @Param("userId") Long userId,
                    @Param("deletedAt") LocalDateTime deletedAt);
}
