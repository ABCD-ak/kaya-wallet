package com.kaya.mapper;

import com.kaya.entity.User;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface UserMapper {

    User findByEmail(@Param("email") String email);

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    int insert(User user);

    int updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    int updateProfile(
            @Param("id") Long id,
            @Param("displayName") String displayName,
            @Param("username") String username,
            @Param("gender") String gender,
            @Param("birthday") LocalDate birthday);

    int updatePassword(@Param("id") Long id, @Param("passwordHash") String passwordHash);

    int updatePaymentPassword(@Param("id") Long id, @Param("paymentPasswordHash") String paymentPasswordHash);

    int updateAvatarUrl(@Param("id") Long id, @Param("avatarUrl") String avatarUrl);
}
