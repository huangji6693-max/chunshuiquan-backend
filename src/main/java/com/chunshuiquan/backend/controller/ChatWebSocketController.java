package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 消息控制器，处理聊天相关的实时通信
 */
@Controller
public class ChatWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketController.class);

    private final MessageRepository messageRepository;
    private final MatchRepository matchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(MessageRepository messageRepository,
                                   MatchRepository matchRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.matchRepository = matchRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 消息已读回执。
     * 客户端发送到 /app/chat/{matchId}/read，
     * 服务端标记该 match 下对方发送的所有消息为已读，
     * 并推送已读回执到 /topic/chat/{matchId}/read
     */
    @MessageMapping("/chat/{matchId}/read")
    @Transactional
    public void markAsRead(@DestinationVariable UUID matchId, Principal principal) {
        if (principal == null) {
            log.warn("已读回执：无法获取用户身份");
            return;
        }

        UUID readerId = UUID.fromString(principal.getName());

        // 验证用户是 match 的参与者
        Match match = matchRepository.findById(matchId).orElse(null);
        if (match == null) {
            log.warn("已读回执：match {} 不存在", matchId);
            return;
        }

        boolean isParticipant = match.getUser1Id().equals(readerId)
                || match.getUser2Id().equals(readerId);
        if (!isParticipant) {
            log.warn("已读回执：用户 {} 不是 match {} 的参与者", readerId, matchId);
            return;
        }

        // 标记对方发送的所有未读消息为已读
        int updated = messageRepository.markAsRead(matchId, readerId);
        if (updated > 0) {
            log.debug("用户 {} 已读 match {} 中 {} 条消息", readerId, matchId, updated);

            // 推送已读回执到 /topic/chat/{matchId}/read
            messagingTemplate.convertAndSend("/topic/chat/" + matchId + "/read",
                    Map.of(
                            "matchId", matchId.toString(),
                            "readerId", readerId.toString(),
                            "readAt", OffsetDateTime.now().toString(),
                            "count", updated
                    ));
        }
    }
}
