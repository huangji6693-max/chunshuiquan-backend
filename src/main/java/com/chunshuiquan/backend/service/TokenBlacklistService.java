package com.chunshuiquan.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token黑名单服务，使用Redis存储已吊销的JWT token。
 * Redis连接失败时fail-open，不阻断正常请求。
 */
@Service
public class TokenBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlacklistService.class);
    private static final String KEY_PREFIX = "token:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将token加入黑名单，TTL设为token剩余过期时间
     *
     * @param token            JWT token
     * @param expirationMillis token剩余有效时间（毫秒）
     */
    public void blacklist(String token, long expirationMillis) {
        if (expirationMillis <= 0) {
            return; // token已过期，无需加入黑名单
        }
        try {
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + token,
                    "1",
                    expirationMillis,
                    TimeUnit.MILLISECONDS
            );
        } catch (Exception e) {
            log.warn("将token加入黑名单失败（Redis不可用）: {}", e.getMessage());
        }
    }

    /**
     * 检查token是否在黑名单中。
     * Redis连接失败时返回false（fail-open），不阻断正常请求。
     */
    public boolean isBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + token));
        } catch (Exception e) {
            log.warn("检查token黑名单失败（Redis不可用）: {}", e.getMessage());
            return false; // fail-open
        }
    }
}
