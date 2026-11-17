package com.kaya.service;

import com.kaya.common.BusinessException;
import com.kaya.dto.auth.AuthTokens;
import com.kaya.dto.auth.EmailLoginRequest;
import com.kaya.dto.auth.LoginRequest;
import com.kaya.dto.auth.RegisterRequest;
import com.kaya.dto.user.UserResponse;
import com.kaya.entity.PasswordResetToken;
import com.kaya.entity.User;
import com.kaya.entity.UserSession;
import com.kaya.mapper.PasswordResetTokenMapper;
import com.kaya.mapper.UserMapper;
import com.kaya.mapper.UserSessionMapper;
import com.kaya.mapper.UserSettingsMapper;
import com.kaya.mapper.WalletMapper;
import com.kaya.security.AuthenticatedUser;
import com.kaya.security.JwtService;
import com.kaya.security.RateLimitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_CURRENCY = "CNY";
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private final UserMapper userMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final WalletMapper walletMapper;
    private final UserSessionMapper userSessionMapper;
    private final PasswordResetTokenMapper passwordResetTokenMapper;
    private final EmailCodeService emailCodeService;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyPasswordHash;
    private final int refreshTokenTtlDays;
    private final int resetTokenTtlMinutes;
    private final String resetPasswordUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserMapper userMapper,
            UserSettingsMapper userSettingsMapper,
            WalletMapper walletMapper,
            UserSessionMapper userSessionMapper,
            PasswordResetTokenMapper passwordResetTokenMapper,
            EmailCodeService emailCodeService,
            RateLimitService rateLimitService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${jwt.refresh-token-ttl-days:30}") int refreshTokenTtlDays,
            @Value("${security.reset-token-ttl-minutes:30}") int resetTokenTtlMinutes,
            @Value("${app.reset-password-url:http://localhost:5173/reset-password}") String resetPasswordUrl) {
        this.userMapper = userMapper;
        this.userSettingsMapper = userSettingsMapper;
        this.walletMapper = walletMapper;
        this.userSessionMapper = userSessionMapper;
        this.passwordResetTokenMapper = passwordResetTokenMapper;
        this.emailCodeService = emailCodeService;
        this.rateLimitService = rateLimitService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
        this.resetTokenTtlMinutes = resetTokenTtlMinutes;
        this.resetPasswordUrl = resetPasswordUrl;
        this.dummyPasswordHash = passwordEncoder.encode("kaya-dummy-password");
    }

    @Transactional
    public AuthTokens register(RegisterRequest request, String userAgent, String ipAddress) {
        String email = normalizeEmail(request.email());
        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException(40901, "该邮箱已注册");
        }
        String username = resolveUsername(request.username(), email);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setUsername(username);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(40901, "邮箱或用户名已存在");
        }

        userSettingsMapper.insertDefault(user.getId(), DEFAULT_CURRENCY);
        walletMapper.insertDefault(user.getId(), DEFAULT_CURRENCY);

        User saved = userMapper.findById(user.getId());
        return issueTokens(saved, userAgent, ipAddress);
    }

    @Transactional
    public AuthTokens login(LoginRequest request, String userAgent, String ipAddress) {
        String email = normalizeEmail(request.email());
        rateLimitService.assertLoginAllowed(email, ipAddress);
        User user = userMapper.findByEmail(email);
        if (user == null) {
            // Keep the response time similar whether or not the email exists.
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            rateLimitService.recordLoginFailure(email, ipAddress);
            throw new BusinessException(40101, "邮箱或密码错误");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            rateLimitService.recordLoginFailure(email, ipAddress);
            throw new BusinessException(40101, "邮箱或密码错误");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(40301, "账号已被禁用或锁定");
        }

        rateLimitService.clearLoginFailures(email, ipAddress);
        LocalDateTime now = utcNow();
        userMapper.updateLastLoginAt(user.getId(), now);
        user.setLastLoginAt(now);
        return issueTokens(user, userAgent, ipAddress);
    }

    @Transactional
    public AuthTokens loginWithEmailCode(EmailLoginRequest request, String userAgent, String ipAddress) {
        User user = emailCodeService.verifyLoginCode(request.email(), request.code());
        LocalDateTime now = utcNow();
        userMapper.updateLastLoginAt(user.getId(), now);
        user.setLastLoginAt(now);
        return issueTokens(user, userAgent, ipAddress);
    }

    @Transactional
    public AuthTokens refresh(String refreshToken, String userAgent, String ipAddress) {
        String hash = sha256(refreshToken);
        UserSession session = userSessionMapper.findByTokenHash(hash);
        LocalDateTime now = utcNow();
        if (session == null || session.getRevokedAt() != null
                || !session.getExpiresAt().isAfter(now)) {
            throw new BusinessException(40102, "刷新令牌无效或已过期");
        }

        User user = userMapper.findById(session.getUserId());
        if (user == null) {
            throw new BusinessException(40102, "刷新令牌无效或已过期");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException(40301, "账号已被禁用或锁定");
        }

        // Rotation: only the first request using this refresh token can revoke it.
        if (userSessionMapper.revokeById(session.getId(), now) == 0) {
            throw new BusinessException(40102, "刷新令牌已失效");
        }
        return issueTokens(user, userAgent, ipAddress);
    }

    @Transactional
    public void logout(String refreshToken, AuthenticatedUser principal) {
        LocalDateTime now = utcNow();
        if (refreshToken != null && !refreshToken.isBlank()) {
            userSessionMapper.revokeByTokenHash(sha256(refreshToken), now);
        }
        if (principal != null && principal.sessionId() != null) {
            userSessionMapper.revokeById(principal.sessionId(), now);
        }
    }

    /**
     * Always returns normally, whether or not the email is registered, to avoid account enumeration.
     * Mail delivery is not wired up yet: the reset link is written to the application log.
     */
    @Transactional
    public void forgotPassword(String email) {
        User user = userMapper.findByEmail(normalizeEmail(email));
        if (user == null) {
            return;
        }

        LocalDateTime now = utcNow();
        passwordResetTokenMapper.invalidateActive(user.getId(), now);

        String rawToken = "prt_" + randomToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(sha256(rawToken));
        token.setExpiresAt(now.plusMinutes(resetTokenTtlMinutes));
        passwordResetTokenMapper.insert(token);

        log.info("Password reset link for {}: {}?token={}",
                user.getEmail(), resetPasswordUrl, rawToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        LocalDateTime now = utcNow();
        PasswordResetToken token = passwordResetTokenMapper.findValidByHash(sha256(rawToken), now);
        if (token == null || passwordResetTokenMapper.markUsed(token.getId(), now) == 0) {
            throw new BusinessException(41001, "重置令牌已过期或已使用");
        }

        userMapper.updatePassword(token.getUserId(), passwordEncoder.encode(newPassword));
        passwordResetTokenMapper.invalidateActive(token.getUserId(), now);
        userSessionMapper.revokeAllExcept(token.getUserId(), null, now);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(40401, "用户不存在");
        }
        return UserResponse.from(user);
    }

    private AuthTokens issueTokens(User user, String userAgent, String ipAddress) {
        SessionToken session = createSession(user.getId(), userAgent, ipAddress);
        String accessToken = jwtService.issueAccessToken(user, session.id());
        return new AuthTokens(
                accessToken,
                session.rawToken(),
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                UserResponse.from(user));
    }

    private SessionToken createSession(Long userId, String userAgent, String ipAddress) {
        String rawToken = "rt_" + randomToken();
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setRefreshTokenHash(sha256(rawToken));
        session.setUserAgent(truncate(userAgent, 255));
        session.setIpAddress(truncate(ipAddress, 45));
        session.setExpiresAt(utcNow().plusDays(refreshTokenTtlDays));
        userSessionMapper.insert(session);
        return new SessionToken(rawToken, session.getId());
    }

    private String resolveUsername(String requested, String email) {
        if (requested != null && !requested.isBlank()) {
            if (userMapper.findByUsername(requested) != null) {
                throw new BusinessException(40902, "用户名已被占用");
            }
            return requested;
        }

        String localPart = email.substring(0, email.indexOf('@'));
        String base = localPart.replaceAll("[^a-zA-Z0-9_]", "_");
        if (base.length() < 3) {
            base = "user_" + base;
        }
        if (base.length() > 24) {
            base = base.substring(0, 24);
        }

        String candidate = base;
        for (int i = 0; i < 10 && userMapper.findByUsername(candidate) != null; i++) {
            candidate = base + "_" + Integer.toHexString(secureRandom.nextInt(0x10000));
        }
        if (userMapper.findByUsername(candidate) != null) {
            throw new BusinessException(40902, "用户名生成失败，请指定用户名");
        }
        return candidate;
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                int b = digest[i] & 0xff;
                hex[i * 2] = HEX[b >>> 4];
                hex[i * 2 + 1] = HEX[b & 0x0f];
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private record SessionToken(String rawToken, Long id) {
    }
}
