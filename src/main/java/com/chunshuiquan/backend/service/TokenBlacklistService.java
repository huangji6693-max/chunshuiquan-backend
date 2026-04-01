package com.chunshuiquan.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务。
 * 有 Redis 时用 Redis，没有 Redis 时用内存 ConcurrentHashMap。
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final boolean redisAvailable;

    // Redis 不可用时的内存兜底
    private final Map<String, Long> memoryBlacklist = new ConcurrentHashMap<>();

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        // 启动时检测 Redis 是否可用
        boolean available;
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            available = true;
            log.info("Redis 连接成功，使用 Redis 存储 token 黑名单");
        } catch (Exception e) {
            available = false;
            log.warn("Redis 不可用，使用内存存储 token 黑名单: {}", e.getMessage());
        }
        this.redisAvailable = available;
    }

    public void blacklist(String token, long expirationMillis) {
        if (expirationMillis <= 0) return;

        if (redisAvailable) {
            try {
                redisTemplate.opsForValue().set(
                        KEY_PREFIX + token, "1", expirationMillis, TimeUnit.MILLISECONDS);
                return;
            } catch (Exception e) {
                log.warn("Redis 黑名单写入失败，降级到内存: {}", e.getMessage());
            }
        }
        memoryBlacklist.put(token, System.currentTimeMillis() + expirationMillis);
    }

    public boolean isBlacklisted(String token) {
        if (redisAvailable) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
            } catch (Exception e) {
                log.warn("Redis 黑名单查询失败: {}", e.getMessage());
            }
        }
        Long expiry = memoryBlacklist.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            memoryBlacklist.remove(token);
            return false;
        }
        return true;
    }
}
