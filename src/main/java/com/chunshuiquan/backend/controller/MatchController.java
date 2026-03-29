package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.MessageRequest;
import com.chunshuiquan.backend.entity.Match;
import com.chunshuiquan.backend.entity.Message;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.MatchRepository;
import com.chunshuiquan.backend.repository.MessageRepository;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private MessageRepository messageRepository;

    // GET /api/matches — 获取我的所有 match 列表
    @GetMapping
    public ResponseEntity<?> getMyMatches(@AuthenticationPrincipal UserDetails userDetails) {
        Profile me = profileRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Match> matches = matchRepository.findActiveMatchesByUserId(me.getId());
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(72);

        List<Map<String, Object>> result = matches.stream().map(match -> {
            // 找出对方的 ID
            UUID otherId = match.getUser1Id().equals(me.getId())
                    ? match.getUser2Id() : match.getUser1Id();

            Map<String, Object> item = new HashMap<>();
            item.put("matchId", match.getId());
            item.put("createdAt", match.getCreatedAt());

            boolean isNew = match.getCreatedAt().isAfter(threshold)
                    && messageRepository.countByMatchId(match.getId()) == 0;
            item.put("isNew", isNew);

            profileRepository.findById(otherId).ifPresent(other -> {
                Map<String, Object> otherInfo = new HashMap<>();
                otherInfo.put("id", other.getId());
                otherInfo.put("email", other.getEmail());
                otherInfo.put("name", other.getName());
                otherInfo.put("bio", other.getBio());
                otherInfo.put("avatarUrls", other.getAvatarUrls());
                otherInfo.put("jobTitle", other.getJobTitle());
                item.put("otherUser", otherInfo);
            });

            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    // POST /api/matches/{matchId}/messages — 发消息
    @PostMapping("/{matchId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable UUID matchId,
            @RequestBody MessageRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Message message = messageService.sendMessage(matchId, userDetails.getUsername(), request.getContent());
            Map<String, Object> resp = new HashMap<>();
            resp.put("id", message.getId());
            resp.put("content", message.getContent());
            resp.put("createdAt", message.getCreatedAt());
            resp.put("senderId", message.getSenderId());
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/matches/{matchId}/messages — 获取聊天记录（降序，最新在前）
    @GetMapping("/{matchId}/messages")
    public ResponseEntity<?> getMessages(
            @PathVariable UUID matchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Page<Message> messages = messageService.getMessages(
                    matchId, userDetails.getUsername(), PageRequest.of(page, size));

            Map<String, Object> resp = new HashMap<>();
            resp.put("content", messages.getContent().stream().map(m -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", m.getId());
                item.put("content", m.getContent());
                item.put("createdAt", m.getCreatedAt());
                item.put("senderId", m.getSenderId());
                item.put("isRead", m.getIsRead());
                return item;
            }).collect(Collectors.toList()));
            resp.put("totalPages", messages.getTotalPages());
            resp.put("totalElements", messages.getTotalElements());
            resp.put("currentPage", page);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
