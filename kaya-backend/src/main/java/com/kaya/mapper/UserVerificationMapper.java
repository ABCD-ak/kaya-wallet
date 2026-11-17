package com.kaya.mapper;

import com.kaya.entity.UserVerification;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserVerificationMapper {

    List<UserVerification> findByUserId(@Param("userId") Long userId);

    UserVerification findByUserAndType(@Param("userId") Long userId, @Param("type") String type);

    int insert(UserVerification verification);
}
