package com.kaya.service;

import com.kaya.common.BusinessException;
import com.kaya.entity.EmailCode;
import com.kaya.entity.User;
import com.kaya.mapper.EmailCodeMapper;
import com.kaya.mapper.UserMapper;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailCodeServiceTest {

    private static final String EMAIL = "nick@example.com";

    @Mock private EmailCodeMapper emailCodeMapper;
    @Mock private UserMapper userMapper;
    @Mock private JavaMailSender mailSender;

    private EmailCodeService emailCodeService;

    @BeforeEach
    void setUp() {
        emailCodeService = new EmailCodeService(
                emailCodeMapper, userMapper, mailSender, "no-reply@kaya.app", "卡雅 Kaya");
    }

    @Test
    @DisplayName("未注册邮箱：静默返回，不发送也不落库")
    void sendCodeUnknownEmailIsSilent() {
        when(userMapper.findByEmail(EMAIL)).thenReturn(null);

        emailCodeService.sendLoginCode(EMAIL, "127.0.0.1");

        verify(emailCodeMapper, never()).insert(any());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("验证码限频：60 秒内重复发送返回 40104")
    void sendCodeTooFrequent() {
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.countSince(eq(EMAIL), eq("login"), any())).thenReturn(1);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.sendLoginCode(EMAIL, "127.0.0.1"));

        assertEquals(40104, ex.getCode());
        verify(emailCodeMapper, never()).insert(any());
    }

    @Test
    @DisplayName("验证码限频：当日达到 10 次返回 40105")
    void sendCodeDailyLimit() {
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.countSince(eq(EMAIL), eq("login"), any())).thenReturn(0, 10);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.sendLoginCode(EMAIL, "127.0.0.1"));

        assertEquals(40105, ex.getCode());
        verify(emailCodeMapper, never()).insert(any());
    }

    @Test
    @DisplayName("验证码发送成功：落库哈希并调用 SMTP")
    void sendCodeSuccess() {
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.countSince(eq(EMAIL), eq("login"), any())).thenReturn(0);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));

        emailCodeService.sendLoginCode(EMAIL, "127.0.0.1");

        verify(emailCodeMapper).insert(any(EmailCode.class));
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("SMTP 失败：返回 50001")
    void sendCodeMailFailure() {
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.countSince(eq(EMAIL), eq("login"), any())).thenReturn(0);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.sendLoginCode(EMAIL, "127.0.0.1"));

        assertEquals(50001, ex.getCode());
    }

    @Test
    @DisplayName("验证码错误：失败次数 +1，返回 40103")
    void verifyWrongCodeIncrementsAttempts() {
        EmailCode record = validRecord("123456");
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.findLatest(EMAIL, "login")).thenReturn(record);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.verifyLoginCode(EMAIL, "000000"));

        assertEquals(40103, ex.getCode());
        verify(emailCodeMapper).incrementAttempts(record.getId());
    }

    @Test
    @DisplayName("验证码正确：一次性消费并返回用户")
    void verifyCorrectCodeMarksUsed() {
        EmailCode record = validRecord("123456");
        User user = activeUser();
        when(userMapper.findByEmail(EMAIL)).thenReturn(user);
        when(emailCodeMapper.findLatest(EMAIL, "login")).thenReturn(record);
        when(emailCodeMapper.markUsed(eq(record.getId()), any())).thenReturn(1);

        User result = emailCodeService.verifyLoginCode(EMAIL, "123456");

        assertSame(user, result);
        verify(emailCodeMapper).markUsed(eq(record.getId()), any());
    }

    @Test
    @DisplayName("验证码已过期：返回 40103")
    void verifyExpiredCode() {
        EmailCode record = validRecord("123456");
        record.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.findLatest(EMAIL, "login")).thenReturn(record);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.verifyLoginCode(EMAIL, "123456"));

        assertEquals(40103, ex.getCode());
    }

    @Test
    @DisplayName("验证码尝试次数达到 5 次：直接失效")
    void verifyTooManyAttempts() {
        EmailCode record = validRecord("123456");
        record.setAttempts(5);
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.findLatest(EMAIL, "login")).thenReturn(record);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.verifyLoginCode(EMAIL, "123456"));

        assertEquals(40103, ex.getCode());
        verify(emailCodeMapper, never()).incrementAttempts(any());
    }

    @Test
    @DisplayName("验证码并发消费：markUsed 影响行数为 0 时返回 40103")
    void verifyCodeConsumedByConcurrentRequest() {
        EmailCode record = validRecord("123456");
        when(userMapper.findByEmail(EMAIL)).thenReturn(activeUser());
        when(emailCodeMapper.findLatest(EMAIL, "login")).thenReturn(record);
        when(emailCodeMapper.markUsed(eq(record.getId()), any())).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                emailCodeService.verifyLoginCode(EMAIL, "123456"));

        assertEquals(40103, ex.getCode());
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail(EMAIL);
        user.setStatus("active");
        return user;
    }

    private EmailCode validRecord(String code) {
        EmailCode record = new EmailCode();
        record.setId(11L);
        record.setEmail(EMAIL);
        record.setPurpose("login");
        record.setCodeHash(sha256(EMAIL + "|" + code));
        record.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        record.setAttempts(0);
        return record;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
