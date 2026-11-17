package com.kaya.security;

import com.kaya.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisPooled;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 登录失败锁定与通用计数限流。
 * 配置 redis.url 时使用 Redis 固定窗口计数；未配置或 Redis 不可用时退化为单机内存，
 * 保证本地开发不依赖 Redis，也避免缓存故障导致登录接口整体不可用。
 */
@Service
public class RateLimitService implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final JedisPooled redis;
    private final int maxLoginFailures;
    private final int loginWindowSeconds;
    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();
    private final AtomicBoolean redisFailureLogged = new AtomicBoolean();

    public RateLimitService(
            @Value("${redis.url:}") String redisUrl,
            @Value("${security.login.max-failures:5}") int maxLoginFailures,
            @Value("${security.login.window-seconds:900}") int loginWindowSeconds) {
        this.maxLoginFailures = maxLoginFailures;
        this.loginWindowSeconds = loginWindowSeconds;
        this.redis = redisUrl == null || redisUrl.isBlank()
                ? null
                : new JedisPooled(URI.create(redisUrl));
    }

    /** 登录前检查：失败次数达到阈值直接拒绝。 */
    public void assertLoginAllowed(String email, String ipAddress) {
        if (currentFailures(loginKey(email, ipAddress)) >= maxLoginFailures) {
            throw new BusinessException(42901, "登录尝试次数过多，请稍后再试");
        }
    }

    /** 记录一次登录失败（邮箱不存在与密码错误一视同仁）。 */
    public void recordLoginFailure(String email, String ipAddress) {
        increment(loginKey(email, ipAddress), loginWindowSeconds);
    }

    /** 登录成功后清除该邮箱 + IP 的失败计数。 */
    public void clearLoginFailures(String email, String ipAddress) {
        delete(loginKey(email, ipAddress));
    }

    long increment(String key, int ttlSeconds) {
        if (redis != null) {
            try {
                long count = redis.incr(key);
                if (count == 1) {
                    redis.expire(key, ttlSeconds);
                }
                return count;
            } catch (RuntimeException ex) {
                logRedisFailure(ex);
            }
        }
        return localIncrement(key, ttlSeconds);
    }

    long currentFailures(String key) {
        if (redis != null) {
            try {
                String value = redis.get(key);
                return value == null ? 0 : Long.parseLong(value);
            } catch (RuntimeException ex) {
                logRedisFailure(ex);
            }
        }
        LocalCounter counter = localCounters.get(key);
        if (counter == null) {
            return 0;
        }
        if (counter.resetAtEpochSecond <= Instant.now().getEpochSecond()) {
            localCounters.remove(key);
            return 0;
        }
        return counter.count;
    }

    void delete(String key) {
        localCounters.remove(key);
        if (redis != null) {
            try {
                redis.del(key);
            } catch (RuntimeException ex) {
                logRedisFailure(ex);
            }
        }
    }

    private long localIncrement(String key, int ttlSeconds) {
        long now = Instant.now().getEpochSecond();
        LocalCounter counter = localCounters.compute(key, (ignored, existing) -> {
            if (existing == null || existing.resetAtEpochSecond <= now) {
                return new LocalCounter(1, now + ttlSeconds);
            }
            return new LocalCounter(existing.count + 1, existing.resetAtEpochSecond);
        });
        return counter.count;
    }

    private String loginKey(String email, String ipAddress) {
        String identity = (email == null ? "" : email.trim().toLowerCase()) + "|" + (ipAddress == null ? "" : ipAddress);
        return "kaya:login:fail:" + sha256(identity).substring(0, 32);
    }

    private void logRedisFailure(RuntimeException ex) {
        if (redisFailureLogged.compareAndSet(false, true)) {
            log.warn("Redis unavailable, falling back to in-memory rate limiting: {}", ex.getMessage());
        }
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
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void destroy() {
        if (redis != null) {
            try {
                redis.close();
            } catch (Exception ex) {
                log.debug("Failed to close Redis client", ex);
            }
        }
    }

    private record LocalCounter(long count, long resetAtEpochSecond) {
    }
}
