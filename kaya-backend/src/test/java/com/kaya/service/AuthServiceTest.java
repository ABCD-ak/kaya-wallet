package com.kaya.service;

import com.kaya.common.BusinessException;
import com.kaya.dto.auth.AuthTokens;
import com.kaya.dto.auth.LoginRequest;
import com.kaya.dto.auth.RegisterRequest;
import com.kaya.entity.PasswordResetToken;
import com.kaya.entity.User;
import com.kaya.entity.UserSession;
import com.kaya.mapper.PasswordResetTokenMapper;
import com.kaya.mapper.UserMapper;
import com.kaya.mapper.UserSessionMapper;
import com.kaya.mapper.UserSettingsMapper;
import com.kaya.mapper.WalletMapper;
import com.kaya.security.JwtService;
import com.kaya.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SESSION_ID = 99L;

    @Mock private UserMapper userMapper;
    @Mock private UserSettingsMapper userSettingsMapper;
    @Mock private WalletMapper walletMapper;
    @Mock private UserSessionMapper userSessionMapper;
    @Mock private PasswordResetTokenMapper passwordResetTokenMapper;
    @Mock private EmailCodeService emailCodeService;
    @Mock private RateLimitService rateLimitService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // 构造函数会用 dummy 哈希初始化“防账号枚举”分支
        when(passwordEncoder.encode("kaya-dummy-password")).thenReturn("dummy-hash");
        authService = new AuthService(
                userMapper, userSettingsMapper, walletMapper, userSessionMapper,
                passwordResetTokenMapper, emailCodeService, rateLimitService, passwordEncoder, jwtService,
                30, 30, "http://localhost:5173/reset-password");
    }

    @Test
    @DisplayName("登录成功：签发访问令牌与刷新令牌，并更新最近登录时间")
    void loginSuccess() {
        User user = activeUser();
        when(userMapper.findByEmail("nick@example.com")).thenReturn(user);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        stubNewSession();
        stubJwt(user);

        AuthTokens tokens = authService.login(
                new LoginRequest("nick@example.com", "secret"), "JUnit", "127.0.0.1");

        assertEquals("access-token", tokens.accessToken());
        assertNotNull(tokens.refreshToken());
        assertTrue(tokens.refreshToken().startsWith("rt_"));
        assertEquals("1", tokens.user().id());
        verify(userMapper).updateLastLoginAt(eq(USER_ID), any());
    }

    @Test
    @DisplayName("登录失败：密码错误返回 40101，且不创建会话")
    void loginWrongPassword() {
        when(userMapper.findByEmail("nick@example.com")).thenReturn(activeUser());
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(new LoginRequest("nick@example.com", "wrong"), null, null));

        assertEquals(40101, ex.getCode());
        verify(userSessionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("登录失败：邮箱不存在时仍执行一次哈希校验，避免账号枚举")
    void loginUnknownEmailRunsDummyHash() {
        when(userMapper.findByEmail("ghost@example.com")).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(new LoginRequest("ghost@example.com", "secret"), null, null));

        assertEquals(40101, ex.getCode());
        verify(passwordEncoder).matches(eq("secret"), anyString());
        verify(userSessionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("登录失败：账号被禁用返回 40301")
    void loginDisabledAccount() {
        User user = activeUser();
        user.setStatus("disabled");
        when(userMapper.findByEmail("nick@example.com")).thenReturn(user);
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(new LoginRequest("nick@example.com", "secret"), null, null));

        assertEquals(40301, ex.getCode());
        verify(userSessionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("刷新令牌成功：吊销旧会话并签发新令牌")
    void refreshRotatesSession() {
        UserSession session = validSession();
        User user = activeUser();
        when(userSessionMapper.findByTokenHash(anyString())).thenReturn(session);
        when(userMapper.findById(USER_ID)).thenReturn(user);
        when(userSessionMapper.revokeById(eq(5L), any())).thenReturn(1);
        stubNewSession();
        stubJwt(user);

        AuthTokens tokens = authService.refresh("old-refresh", null, null);

        assertEquals("access-token", tokens.accessToken());
        assertTrue(tokens.refreshToken().startsWith("rt_"));
        verify(userSessionMapper).revokeById(eq(5L), any());
    }

    @Test
    @DisplayName("刷新令牌重放：旧令牌已被并发请求吊销时返回 40102")
    void refreshReplayRejected() {
        when(userSessionMapper.findByTokenHash(anyString())).thenReturn(validSession());
        when(userMapper.findById(USER_ID)).thenReturn(activeUser());
        when(userSessionMapper.revokeById(eq(5L), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.refresh("old-refresh", null, null));

        assertEquals(40102, ex.getCode());
        verify(jwtService, never()).issueAccessToken(any(), anyLong());
    }

    @Test
    @DisplayName("刷新令牌过期：返回 40102")
    void refreshExpiredToken() {
        UserSession session = validSession();
        session.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        when(userSessionMapper.findByTokenHash(anyString())).thenReturn(session);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.refresh("old-refresh", null, null));

        assertEquals(40102, ex.getCode());
        verify(userSessionMapper, never()).revokeById(anyLong(), any());
    }

    @Test
    @DisplayName("登录失败：失败次数达到阈值返回 42901，且不再查询数据库")
    void loginBlockedByRateLimit() {
        org.mockito.Mockito.doThrow(new BusinessException(42901, "登录尝试次数过多，请稍后再试"))
                .when(rateLimitService).assertLoginAllowed("nick@example.com", "127.0.0.1");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.login(new LoginRequest("nick@example.com", "secret"), null, "127.0.0.1"));

        assertEquals(42901, ex.getCode());
        verify(userMapper, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("注册失败：邮箱已存在返回 40901")
    void registerDuplicateEmail() {
        when(userMapper.findByEmail("nick@example.com")).thenReturn(activeUser());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.register(new RegisterRequest("Nick", "nick@example.com", "password123", null),
                        null, null));

        assertEquals(40901, ex.getCode());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册失败：指定用户名已被占用返回 40902")
    void registerDuplicateUsername() {
        when(userMapper.findByEmail("new@example.com")).thenReturn(null);
        when(userMapper.findByUsername("nick")).thenReturn(activeUser());

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.register(new RegisterRequest("Nick", "new@example.com", "password123", "nick"),
                        null, null));

        assertEquals(40902, ex.getCode());
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("注册成功：创建默认设置与钱包，并直接返回登录态")
    void registerCreatesDefaultResources() {
        when(userMapper.findByEmail("new@example.com")).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("new-hash");
        when(userMapper.insert(any())).thenAnswer(invocation -> {
            invocation.<User>getArgument(0).setId(USER_ID);
            return 1;
        });
        User saved = activeUser();
        when(userMapper.findById(USER_ID)).thenReturn(saved);
        stubNewSession();
        stubJwt(saved);

        AuthTokens tokens = authService.register(
                new RegisterRequest("Nick", "new@example.com", "password123", null), null, null);

        assertNotNull(tokens.accessToken());
        verify(userSettingsMapper).insertDefault(USER_ID, "CNY");
        verify(walletMapper).insertDefault(USER_ID, "CNY");
    }

    @Test
    @DisplayName("忘记密码：邮箱不存在时静默返回，不写重置令牌")
    void forgotPasswordDoesNotLeakAccount() {
        when(userMapper.findByEmail("ghost@example.com")).thenReturn(null);

        authService.forgotPassword("ghost@example.com");

        verify(passwordResetTokenMapper, never()).insert(any());
    }

    @Test
    @DisplayName("重置密码：令牌无效返回 41001")
    void resetPasswordInvalidToken() {
        when(passwordResetTokenMapper.findValidByHash(anyString(), any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.resetPassword("bad-token", "new-password"));

        assertEquals(41001, ex.getCode());
        verify(userMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("重置密码成功：更新密码并吊销全部会话")
    void resetPasswordRevokesAllSessions() {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(7L);
        token.setUserId(USER_ID);
        when(passwordResetTokenMapper.findValidByHash(anyString(), any())).thenReturn(token);
        when(passwordResetTokenMapper.markUsed(eq(7L), any())).thenReturn(1);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        authService.resetPassword("valid-token", "new-password");

        verify(userMapper).updatePassword(USER_ID, "new-hash");
        verify(passwordResetTokenMapper).invalidateActive(eq(USER_ID), any());
        verify(userSessionMapper).revokeAllExcept(eq(USER_ID), isNull(), any());
    }

    private User activeUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail("nick@example.com");
        user.setPasswordHash("hash");
        user.setDisplayName("Nick");
        user.setUsername("nick");
        user.setStatus("active");
        user.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        user.setUpdatedAt(LocalDateTime.now(ZoneOffset.UTC));
        return user;
    }

    private UserSession validSession() {
        UserSession session = new UserSession();
        session.setId(5L);
        session.setUserId(USER_ID);
        session.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
        return session;
    }

    private void stubNewSession() {
        when(userSessionMapper.insert(any())).thenAnswer(invocation -> {
            invocation.<UserSession>getArgument(0).setId(SESSION_ID);
            return 1;
        });
    }

    private void stubJwt(User user) {
        when(jwtService.issueAccessToken(eq(user), anyLong())).thenReturn("access-token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(3600L);
    }
}
