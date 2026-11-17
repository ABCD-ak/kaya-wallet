package com.kaya.service;

import com.kaya.common.BusinessException;
import com.kaya.entity.EmailCode;
import com.kaya.entity.User;
import com.kaya.mapper.EmailCodeMapper;
import com.kaya.mapper.UserMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

/**
 * 邮箱验证码：生成、限频发送、校验并一次性消费。
 * 验证码只存 SHA-256，不存明文。
 */
@Service
public class EmailCodeService {

    private static final Logger log = LoggerFactory.getLogger(EmailCodeService.class);
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static final String PURPOSE_LOGIN = "login";
    private static final int CODE_TTL_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private static final int DAILY_LIMIT = 10;

    private final EmailCodeMapper emailCodeMapper;
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailCodeService(
            EmailCodeMapper emailCodeMapper,
            UserMapper userMapper,
            JavaMailSender mailSender,
            @Value("${mail.from}") String fromAddress,
            @Value("${mail.from-name:卡雅 Kaya}") String fromName) {
        this.emailCodeMapper = emailCodeMapper;
        this.userMapper = userMapper;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    /**
     * 发送登录验证码。邮箱未注册时静默返回，避免被用来探测账号。
     */
    @Transactional
    public void sendLoginCode(String rawEmail, String ipAddress) {
        String email = normalizeEmail(rawEmail);
        User user = userMapper.findByEmail(email);
        if (user == null || !"active".equals(user.getStatus())) {
            return;
        }

        LocalDateTime now = utcNow();
        if (emailCodeMapper.countSince(email, PURPOSE_LOGIN,
                now.minusSeconds(RESEND_INTERVAL_SECONDS)) > 0) {
            throw new BusinessException(40104, "发送太频繁，请 1 分钟后再试");
        }
        if (emailCodeMapper.countSince(email, PURPOSE_LOGIN, now.minusHours(24)) >= DAILY_LIMIT) {
            throw new BusinessException(40105, "今日验证码次数已达上限，请明天再试");
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        EmailCode record = new EmailCode();
        record.setEmail(email);
        record.setCodeHash(sha256(email + "|" + code));
        record.setPurpose(PURPOSE_LOGIN);
        record.setExpiresAt(now.plusMinutes(CODE_TTL_MINUTES));
        record.setIpAddress(truncate(ipAddress, 45));
        emailCodeMapper.insert(record);

        try {
            sendCodeMail(email, code);
        } catch (Exception ex) {
            log.error("Failed to send login code to {}", email, ex);
            throw new BusinessException(50001, "验证码邮件发送失败，请稍后再试");
        }
    }

    /**
     * 校验验证码并消费。成功返回用户，失败统一抛出“验证码错误或已过期”。
     */
    @Transactional
    public User verifyLoginCode(String rawEmail, String code) {
        String email = normalizeEmail(rawEmail);
        User user = userMapper.findByEmail(email);
        if (user == null || !"active".equals(user.getStatus())) {
            throw new BusinessException(40103, "验证码错误或已过期");
        }

        LocalDateTime now = utcNow();
        EmailCode record = emailCodeMapper.findLatest(email, PURPOSE_LOGIN);
        if (record == null
                || record.getUsedAt() != null
                || !record.getExpiresAt().isAfter(now)
                || record.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessException(40103, "验证码错误或已过期");
        }

        if (code == null || !sha256(email + "|" + code.trim()).equals(record.getCodeHash())) {
            emailCodeMapper.incrementAttempts(record.getId());
            throw new BusinessException(40103, "验证码错误或已过期");
        }

        if (emailCodeMapper.markUsed(record.getId(), now) == 0) {
            throw new BusinessException(40103, "验证码错误或已过期");
        }
        return user;
    }

    private void sendCodeMail(String to, String code) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject("卡雅 Kaya · 登录验证码");
        helper.setText(buildHtml(code), true);
        mailSender.send(message);
    }

    private String buildHtml(String code) {
        return """
                <div style="margin:0;padding:32px 0;background:#f7f6f8;font-family:-apple-system,'PingFang SC','Microsoft YaHei',sans-serif;">
                  <div style="max-width:440px;margin:0 auto;padding:32px 28px;background:#ffffff;border-radius:20px;">
                    <div style="font-size:13px;letter-spacing:4px;color:#9aa5a2;">KAYA</div>
                    <h2 style="margin:18px 0 8px;font-size:20px;color:#1d2b29;">登录验证码</h2>
                    <p style="margin:0;font-size:13px;line-height:1.8;color:#7c8886;">请输入下面的验证码完成登录，5 分钟内有效。</p>
                    <div style="margin:24px 0;padding:16px;border-radius:14px;background:#f2fbf9;text-align:center;">
                      <span style="font-size:32px;font-weight:700;letter-spacing:10px;color:#17b7bd;">%s</span>
                    </div>
                    <p style="margin:0;font-size:11px;line-height:1.8;color:#b0b9b7;">如果不是你本人操作，请忽略这封邮件，你的账号不会有任何变化。</p>
                  </div>
                </div>
                """.formatted(code);
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
}
