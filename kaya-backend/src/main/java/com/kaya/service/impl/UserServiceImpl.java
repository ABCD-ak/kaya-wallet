package com.kaya.service.impl;

import com.kaya.common.BusinessException;
import com.kaya.dto.user.AvatarResponse;
import com.kaya.dto.user.ChangePasswordRequest;
import com.kaya.dto.user.PaymentPasswordStatusResponse;
import com.kaya.dto.user.ResetPaymentPasswordRequest;
import com.kaya.dto.user.SetPaymentPasswordRequest;
import com.kaya.dto.user.UpdateProfileRequest;
import com.kaya.dto.user.UserResponse;
import com.kaya.entity.User;
import com.kaya.mapper.UserMapper;
import com.kaya.mapper.UserSessionMapper;
import com.kaya.service.UserService;
import com.kaya.storage.AvatarStorage;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserSessionMapper userSessionMapper;
    private final PasswordEncoder passwordEncoder;
    private final AvatarStorage avatarStorage;

    public UserServiceImpl(
            UserMapper userMapper,
            UserSessionMapper userSessionMapper,
            PasswordEncoder passwordEncoder,
            AvatarStorage avatarStorage) {
        this.userMapper = userMapper;
        this.userSessionMapper = userSessionMapper;
        this.passwordEncoder = passwordEncoder;
        this.avatarStorage = avatarStorage;
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        if (request.email() != null) {
            throw new BusinessException(40001, "邮箱是登录凭证，修改需要单独验证，暂不支持");
        }

        String displayName = trimToNull(request.displayName());
        String username = trimToNull(request.username());
        String gender = trimToNull(request.gender());
        LocalDate birthday = request.birthday();

        if (request.displayName() != null && displayName == null) {
            throw new BusinessException(40001, "昵称不能为空");
        }
        if (displayName == null && username == null && gender == null && birthday == null) {
            return UserResponse.from(user);
        }

        if (username != null && !username.equals(user.getUsername())) {
            User existing = userMapper.findByUsername(username);
            if (existing != null && !existing.getId().equals(userId)) {
                throw new BusinessException(40902, "用户名已被占用");
            }
        }

        try {
            userMapper.updateProfile(userId, displayName, username, gender, birthday);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40902, "用户名已被占用");
        }

        return UserResponse.from(userMapper.findById(userId));
    }

    @Override
    @Transactional
    public void changePassword(Long userId, Long sessionId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(40002, "当前密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(40001, "新密码不能与当前密码相同");
        }

        userMapper.updatePassword(userId, passwordEncoder.encode(request.newPassword()));
        // Keep the current session alive, revoke every other refresh token.
        userSessionMapper.revokeAllExcept(userId, sessionId, LocalDateTime.now(ZoneOffset.UTC));
    }

    @Override
    @Transactional
    public AvatarResponse updateAvatar(Long userId, MultipartFile file) {
        requireUser(userId);
        String avatarUrl = avatarStorage.store(file);
        userMapper.updateAvatarUrl(userId, avatarUrl);
        return new AvatarResponse(avatarUrl);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentPasswordStatusResponse getPaymentPasswordStatus(Long userId) {
        return PaymentPasswordStatusResponse.of(hasPaymentPassword(userId));
    }

    @Override
    @Transactional
    public void setPaymentPassword(Long userId, SetPaymentPasswordRequest request) {
        User user = requireUser(userId);
        boolean alreadySet = user.getPaymentPasswordHash() != null && !user.getPaymentPasswordHash().isBlank();
        if (alreadySet) {
            if (request.currentPassword() == null || request.currentPassword().isBlank()) {
                throw new BusinessException(40001, "请输入当前支付密码");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPaymentPasswordHash())) {
                throw new BusinessException(40002, "当前支付密码不正确");
            }
        }
        if (alreadySet && passwordEncoder.matches(request.newPassword(), user.getPaymentPasswordHash())) {
            throw new BusinessException(40001, "新支付密码不能与当前支付密码相同");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(40001, "支付密码不能与登录密码相同");
        }
        userMapper.updatePaymentPassword(userId, passwordEncoder.encode(request.newPassword()));
    }

    @Override
    @Transactional
    public void resetPaymentPassword(Long userId, ResetPaymentPasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.loginPassword(), user.getPasswordHash())) {
            throw new BusinessException(40002, "登录密码不正确");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(40001, "支付密码不能与登录密码相同");
        }
        userMapper.updatePaymentPassword(userId, passwordEncoder.encode(request.newPassword()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPaymentPassword(Long userId) {
        User user = requireUser(userId);
        return user.getPaymentPasswordHash() != null && !user.getPaymentPasswordHash().isBlank();
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyPaymentPassword(Long userId, String rawPaymentPassword) {
        User user = requireUser(userId);
        String hash = user.getPaymentPasswordHash();
        if (hash == null || hash.isBlank()) {
            throw new BusinessException(40001, "尚未设置支付密码");
        }
        if (rawPaymentPassword == null || !passwordEncoder.matches(rawPaymentPassword, hash)) {
            throw new BusinessException(40005, "支付密码不正确");
        }
    }

    private User requireUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }
        return user;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
