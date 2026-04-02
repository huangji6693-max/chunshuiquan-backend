package com.chunshuiquan.backend.config;

import com.chunshuiquan.backend.security.WebSocketAuthInterceptor;
import com.chunshuiquan.backend.service.OnlineStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * WebSocket 配置，使用 STOMP 协议
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${cors.allowed-origins:*}")
    private String corsOrigins;

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final OnlineStatusService onlineStatusService;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor,
                           OnlineStatusService onlineStatusService) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.onlineStatusService = onlineStatusService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 订阅前缀：客户端通过 /topic/... 订阅消息
        registry.enableSimpleBroker("/topic");
        // 应用目的地前缀：客户端通过 /app/... 发送消息到服务端
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 端点，允许所有 origin，支持 SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsOrigins.split(","))
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 注册认证拦截器，在 CONNECT 帧时验证 JWT
        registration.interceptors(webSocketAuthInterceptor);
    }

    /**
     * 用户 WebSocket 连接成功时标记在线
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            onlineStatusService.markOnline(principal.getName());
            log.info("用户 {} WebSocket 已连接，标记为在线", principal.getName());
        }
    }

    /**
     * 用户 WebSocket 断开时标记离线
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal != null) {
            onlineStatusService.markOffline(principal.getName());
            log.info("用户 {} WebSocket 已断开，标记为离线", principal.getName());
        }
    }
}
