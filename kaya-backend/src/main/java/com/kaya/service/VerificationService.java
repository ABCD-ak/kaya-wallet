package com.kaya.service;

import com.kaya.dto.user.CreateVerificationRequest;
import com.kaya.dto.user.VerificationResponse;

import java.util.List;

public interface VerificationService {

    List<VerificationResponse> list(Long userId);

    VerificationResponse submit(Long userId, CreateVerificationRequest request);
}
