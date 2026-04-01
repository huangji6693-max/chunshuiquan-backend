package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.dto.MessageResponseDto;
import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Message;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.MessageRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PushNotificationService pushNotificationService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public Message sendMessage(UUID matchId, String senderId, String content) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Profile sender = profileRepository.findById(UUID.fromString(senderId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 确认发送者是 match 的参与者
        boolean isParticipant = match.getUser1Id().equals(sender.getId())
                || match.getUser2Id().equals(sender.getId());
        if (!isParticipant) {
            throw new RuntimeException("Not a participant of this match");
        }

        Message message = new Message();
        message.setMatchId(matchId);
        message.setSenderId(sender.getId());
        message.setContent(content);

        Message saved = messageRepository.save(message);

        // 通过 WebSocket 实时推送消息到 /topic/chat/{matchId}
        MessageResponseDto wsPayload = new MessageResponseDto(
                saved.getId(), saved.getContent(),
                saved.getCreatedAt(), saved.getSenderId(), saved.getIsRead());
        messagingTemplate.convertAndSend("/topic/chat/" + matchId, wsPayload);

        // 通过 FCM 通知对方有新消息
        UUID receiverId = match.getUser1Id().equals(sender.getId())
                ? match.getUser2Id() : match.getUser1Id();
        profileRepository.findById(receiverId).ifPresent(receiver -> {
            String preview = content.length() > 50 ? content.substring(0, 50) + "…" : content;
            pushNotificationService.sendNewMessageNotification(
                    receiver.getFcmToken(), sender.getName(), preview, matchId.toString());
        });

        return saved;
    }

    public Page<Message> getMessages(UUID matchId, String userId, Pageable pageable) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Profile user = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isParticipant = match.getUser1Id().equals(user.getId())
                || match.getUser2Id().equals(user.getId());
        if (!isParticipant) {
            throw new RuntimeException("Not a participant of this match");
        }

        return messageRepository.findByMatchIdOrderByCreatedAtDesc(matchId, pageable);
    }
}
