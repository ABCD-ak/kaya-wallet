package com.kaya.security;

import com.kaya.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "another-unit-test-secret-key-32-bytes";

    @Test
    @DisplayName("密钥不足 32 字节时拒绝启动")
    void rejectsShortSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", "kaya", 3600));
    }

    @Test
    @DisplayName("签发后可解析出用户、会话与过期时间")
    void issueAndParseRoundTrip() {
        JwtService service = new JwtService(SECRET, "kaya", 3600);

        String token = service.issueAccessToken(user(), 7L);
        AuthenticatedUser principal = service.parseAccessToken(token);

        assertEquals(42L, principal.id());
        assertEquals("nick@example.com", principal.email());
        assertEquals("nick", principal.username());
        assertEquals(7L, principal.sessionId());
        assertEquals(3600, service.getAccessTokenTtlSeconds());
    }

    @Test
    @DisplayName("篡改或使用其它密钥签发的令牌无法通过校验")
    void rejectsTamperedToken() {
        JwtService issuer = new JwtService(SECRET, "kaya", 3600);
        JwtService verifier = new JwtService(OTHER_SECRET, "kaya", 3600);
        String token = issuer.issueAccessToken(user(), 7L);

        assertThrows(JwtException.class, () -> verifier.parseAccessToken(token));
    }

    @Test
    @DisplayName("issuer 不匹配时拒绝解析")
    void rejectsWrongIssuer() {
        JwtService issuer = new JwtService(SECRET, "kaya", 3600);
        JwtService verifier = new JwtService(SECRET, "another-app", 3600);

        String token = issuer.issueAccessToken(user(), 7L);

        assertThrows(JwtException.class, () -> verifier.parseAccessToken(token));
    }

    @Test
    @DisplayName("过期令牌拒绝解析")
    void rejectsExpiredToken() {
        JwtService service = new JwtService(SECRET, "kaya", -1);
        String token = service.issueAccessToken(user(), 7L);

        assertThrows(ExpiredJwtException.class, () -> service.parseAccessToken(token));
    }

    private User user() {
        User user = new User();
        user.setId(42L);
        user.setEmail("nick@example.com");
        user.setUsername("nick");
        return user;
    }
}
