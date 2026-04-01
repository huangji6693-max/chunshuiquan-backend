package com.chunshuiquan.backend.security;

import com.chunshuiquan.backend.service.TokenBlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * WebSocket STOMP 连接认证拦截器。
 * 在 CONNECT 帧时从 header 提取 JWT token 进行认证，
 * 认证成功后将 userId 设置为 Principal。
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    public WebSocketAuthInterceptor(JwtUtil jwtUtil, TokenBlacklistService tokenBlacklistService) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从 STOMP header 中提取 token（客户端通过 Authorization header 传递）
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT 缺少有效的 Authorization header，拒绝连接");
                throw new SecurityException("WebSocket 认证失败：缺少 token");
            }

            String token = authHeader.substring(7);
            if (!jwtUtil.isValid(token) || tokenBlacklistService.isBlacklisted(token)) {
                log.warn("WebSocket CONNECT token 无效或已被吊销，拒绝连接");
                throw new SecurityException("WebSocket 认证失败：token 无效");
            }

            String userId = jwtUtil.extractUserId(token);
            // 将 userId 设置为 Principal，后续可通过 Principal 获取用户身份
            accessor.setUser(new StompPrincipal(userId));
            log.debug("WebSocket CONNECT 认证成功，userId={}", userId);
        }
        return message;
    }

    /**
     * 简单的 Principal 实现，用于在 WebSocket 会话中标识用户
     */
    private static class StompPrincipal implements Principal {
        private final String name;

        StompPrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
