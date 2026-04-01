package com.chunshuiquan.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 在线状态服务，使用 Redis 存储用户在线状态。
 * 所有 Redis 操作均 fail-open，不影响正常业务。
 */
@Service
public class OnlineStatusService {

    private static final Logger log = LoggerFactory.getLogger(OnlineStatusService.class);

    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final String LAST_SEEN_KEY_PREFIX = "user:lastseen:";
    private static final long ONLINE_TTL_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public OnlineStatusService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 标记用户为在线，设置 60 秒 TTL
     */
    public void markOnline(String userId) {
        try {
            redisTemplate.opsForValue().set(
                    ONLINE_KEY_PREFIX + userId, "1",
                    ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
            // 同时更新最后在线时间
            redisTemplate.opsForValue().set(
                    LAST_SEEN_KEY_PREFIX + userId,
                    OffsetDateTime.now().toString());
        } catch (Exception e) {
            log.warn("标记用户 {} 在线失败（Redis不可用）: {}", userId, e.getMessage());
        }
    }

    /**
     * 标记用户离线，删除在线标记，更新最后在线时间
     */
    public void markOffline(String userId) {
        try {
            redisTemplate.delete(ONLINE_KEY_PREFIX + userId);
            redisTemplate.opsForValue().set(
                    LAST_SEEN_KEY_PREFIX + userId,
                    OffsetDateTime.now().toString());
        } catch (Exception e) {
            log.warn("标记用户 {} 离线失败（Redis不可用）: {}", userId, e.getMessage());
        }
    }

    /**
     * 检查用户是否在线。Redis 不可用时返回 false（fail-open）。
     */
    public boolean isOnline(String userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(ONLINE_KEY_PREFIX + userId));
        } catch (Exception e) {
            log.warn("检查用户 {} 在线状态失败（Redis不可用）: {}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户最后在线时间。Redis 不可用时返回 null（fail-open）。
     */
    public String getLastSeen(String userId) {
        try {
            return redisTemplate.opsForValue().get(LAST_SEEN_KEY_PREFIX + userId);
        } catch (Exception e) {
            log.warn("获取用户 {} 最后在线时间失败（Redis不可用）: {}", userId, e.getMessage());
            return null;
        }
    }
}
