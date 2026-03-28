package com.chunshuiquan.backend.service;

import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Message;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.MessageRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Message sendMessage(UUID matchId, String senderEmail, String content) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Profile sender = profileRepository.findByEmail(senderEmail)
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

        return messageRepository.save(message);
    }

    public Page<Message> getMessages(UUID matchId, String userEmail, Pageable pageable) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        Profile user = profileRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isParticipant = match.getUser1Id().equals(user.getId())
                || match.getUser2Id().equals(user.getId());
        if (!isParticipant) {
            throw new RuntimeException("Not a participant of this match");
        }

        return messageRepository.findByMatchIdOrderByCreatedAtDesc(matchId, pageable);
    }
}
