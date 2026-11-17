package com.kaya.service;

import com.kaya.dto.user.AvatarResponse;
import com.kaya.dto.user.ChangePasswordRequest;
import com.kaya.dto.user.PaymentPasswordStatusResponse;
import com.kaya.dto.user.ResetPaymentPasswordRequest;
import com.kaya.dto.user.SetPaymentPasswordRequest;
import com.kaya.dto.user.UpdateProfileRequest;
import com.kaya.dto.user.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse updateProfile(Long userId, UpdateProfileRequest request);

    void changePassword(Long userId, Long sessionId, ChangePasswordRequest request);

    AvatarResponse updateAvatar(Long userId, MultipartFile file);

    PaymentPasswordStatusResponse getPaymentPasswordStatus(Long userId);

    void setPaymentPassword(Long userId, SetPaymentPasswordRequest request);

    void resetPaymentPassword(Long userId, ResetPaymentPasswordRequest request);

    boolean hasPaymentPassword(Long userId);

    void verifyPaymentPassword(Long userId, String rawPaymentPassword);
}
