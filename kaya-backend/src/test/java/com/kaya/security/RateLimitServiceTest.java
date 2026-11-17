package com.kaya.security;

import com.kaya.common.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitServiceTest {

    @Test
    @DisplayName("未配置 Redis 时使用内存计数：5 次失败后锁定")
    void locksAfterMaxFailuresWithoutRedis() {
        RateLimitService service = new RateLimitService("", 5, 900);

        for (int i = 0; i < 4; i++) {
            service.recordLoginFailure("nick@example.com", "127.0.0.1");
        }
        assertDoesNotThrow(() -> service.assertLoginAllowed("nick@example.com", "127.0.0.1"));

        service.recordLoginFailure("nick@example.com", "127.0.0.1");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.assertLoginAllowed("nick@example.com", "127.0.0.1"));
        assertEquals(42901, ex.getCode());
    }

    @Test
    @DisplayName("登录成功后清除失败计数")
    void clearResetsCounter() {
        RateLimitService service = new RateLimitService("", 2, 900);
        service.recordLoginFailure("nick@example.com", "127.0.0.1");
        service.recordLoginFailure("nick@example.com", "127.0.0.1");

        service.clearLoginFailures("nick@example.com", "127.0.0.1");

        assertDoesNotThrow(() -> service.assertLoginAllowed("nick@example.com", "127.0.0.1"));
        assertEquals(0, service.currentFailures(loginKey(service, "nick@example.com", "127.0.0.1")));
    }

    @Test
    @DisplayName("不同 IP 的失败次数互不影响")
    void countersAreScopedByIp() {
        RateLimitService service = new RateLimitService("", 1, 900);
        service.recordLoginFailure("nick@example.com", "10.0.0.1");

        assertThrows(BusinessException.class,
                () -> service.assertLoginAllowed("nick@example.com", "10.0.0.1"));
        assertDoesNotThrow(() -> service.assertLoginAllowed("nick@example.com", "10.0.0.2"));
    }

    private String loginKey(RateLimitService service, String email, String ip) {
        // 与实现保持一致的 key 规则，仅用于断言内部计数
        return "kaya:login:fail:" + sha256(email + "|" + ip).substring(0, 32);
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
