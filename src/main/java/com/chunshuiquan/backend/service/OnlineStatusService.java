package com.chunshuiquan.backend.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线状态服务 — 纯内存实现（无Redis依赖）
 */
@Service
public class OnlineStatusService {

    private static final long ONLINE_TTL_MS = 60_000; // 60秒
    private final Map<String, Long> onlineMap = new ConcurrentHashMap<>();
    private final Map<String, String> lastSeenMap = new ConcurrentHashMap<>();

    public void markOnline(String userId) {
        onlineMap.put(userId, System.currentTimeMillis() + ONLINE_TTL_MS);
        lastSeenMap.put(userId, OffsetDateTime.now().toString());
    }

    public void markOffline(String userId) {
        onlineMap.remove(userId);
        lastSeenMap.put(userId, OffsetDateTime.now().toString());
    }

    public boolean isOnline(String userId) {
        Long expiry = onlineMap.get(userId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            onlineMap.remove(userId);
            return false;
        }
        return true;
    }

    public String getLastSeen(String userId) {
        return lastSeenMap.get(userId);
    }
}
