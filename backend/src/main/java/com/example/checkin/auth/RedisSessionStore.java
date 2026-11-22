package com.example.checkin.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 登录态存储。key 使用 token 的 SHA-256 摘要，避免原始 token 落盘；值为 userId。
 */
@Component
public class RedisSessionStore {

    private static final String KEY_PREFIX = "checkin:auth:session:";
    private static final Duration TTL = Duration.ofHours(12);

    private final StringRedisTemplate redis;

    public RedisSessionStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void save(String token, Long userId) {
        redis.opsForValue().set(key(token), userId.toString(), TTL);
    }

    /**
     * 读取会话。Redis 异常向上抛，由认证过滤器统一转为 503。
     */
    public Long find(String token) {
        String value = redis.opsForValue().get(key(token));
        return value == null ? null : Long.valueOf(value);
    }

    public void delete(String token) {
        redis.delete(key(token));
    }

    private String key(String token) {
        return KEY_PREFIX + sha256(token);
    }

    private String sha256(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}