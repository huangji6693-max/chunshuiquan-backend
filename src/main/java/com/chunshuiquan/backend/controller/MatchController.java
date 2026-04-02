package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.MatchItemDto;
import com.chunshuiquan.backend.dto.MessageRequest;
import com.chunshuiquan.backend.dto.MessageResponseDto;
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
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    // GET /api/matches — 获取我的所有 match 列表（批量查询，消除N+1）
    @GetMapping
    public ResponseEntity<List<MatchItemDto>> getMyMatches(@AuthenticationPrincipal String userId) {
        Profile me = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Match> matches = matchRepository.findActiveMatchesByUserId(me.getId());
        if (matches.isEmpty()) return ResponseEntity.ok(Collections.emptyList());

        OffsetDateTime threshold = OffsetDateTime.now().minusHours(72);

        // 批量查询所有对方 Profile（1次查询替代N次）
        List<UUID> otherIds = matches.stream()
                .map(m -> m.getUser1Id().equals(me.getId()) ? m.getUser2Id() : m.getUser1Id())
                .toList();
        Map<UUID, Profile> profileMap = profileRepository.findAllById(otherIds).stream()
                .collect(Collectors.toMap(Profile::getId, p -> p));

        // 批量查询最后一条消息（1次查询替代N次）
        List<UUID> matchIds = matches.stream().map(Match::getId).toList();
        Map<UUID, Message> lastMsgMap = messageRepository.findLastMessagesByMatchIds(matchIds).stream()
                .collect(Collectors.toMap(Message::getMatchId, m -> m));

        // 批量查询未读消息数（1次查询替代N次）
        Map<UUID, Integer> unreadMap = new HashMap<>();
        for (Object[] row : messageRepository.countUnreadByMatchIds(matchIds, me.getId())) {
            unreadMap.put((UUID) row[0], ((Number) row[1]).intValue());
        }

        // 批量查询消息总数（1次查询替代N次，用于判断新匹配）
        Map<UUID, Long> msgCountMap = new HashMap<>();
        for (Object[] row : messageRepository.countByMatchIds(matchIds)) {
            msgCountMap.put((UUID) row[0], ((Number) row[1]).longValue());
        }

        // 组装DTO——零额外查询
        List<MatchItemDto> result = matches.stream().map(match -> {
            UUID otherId = match.getUser1Id().equals(me.getId())
                    ? match.getUser2Id() : match.getUser1Id();

            boolean isNew = match.getCreatedAt().isAfter(threshold)
                    && msgCountMap.getOrDefault(match.getId(), 0L) == 0;

            Profile other = profileMap.get(otherId);
            MatchItemDto.OtherUserDto otherDto = other != null
                    ? new MatchItemDto.OtherUserDto(
                            other.getId(), other.getEmail(), other.getName(),
                            other.getBio(),
                            other.getAvatarUrls() != null ? Arrays.asList(other.getAvatarUrls()) : Collections.emptyList(),
                            other.getJobTitle(),
                            other.getHeight(), other.getEducation(), other.getZodiac(),
                            other.getCity(), other.getSmoking(), other.getDrinking(),
                            other.getVipTier())
                    : null;

            String lastMessage = null;
            OffsetDateTime lastMessageAt = null;
            Message lastMsg = lastMsgMap.get(match.getId());
            if (lastMsg != null) {
                lastMessage = lastMsg.getContent() != null && lastMsg.getContent().length() > 50
                        ? lastMsg.getContent().substring(0, 50) + "..."
                        : lastMsg.getContent();
                lastMessageAt = lastMsg.getCreatedAt();
            }

            int unreadCount = unreadMap.getOrDefault(match.getId(), 0);

            return new MatchItemDto(match.getId(), match.getCreatedAt(), isNew, otherDto,
                    lastMessage, lastMessageAt, unreadCount);
        }).collect(Collectors.toList());

        result.sort(Comparator.comparing(
                (MatchItemDto dto) -> dto.getLastMessageAt() != null ? dto.getLastMessageAt() : dto.getCreatedAt()
        ).reversed());

        return ResponseEntity.ok(result);
    }

    // POST /api/matches/{matchId}/messages — 发消息
    @PostMapping("/{matchId}/messages")
    public ResponseEntity<?> sendMessage(
            @PathVariable UUID matchId,
            @RequestBody MessageRequest request,
            @AuthenticationPrincipal String userId) {
        try {
            Message message = messageService.sendMessage(matchId, userId, request.getContent());
            MessageResponseDto resp = new MessageResponseDto(
                    message.getId(), message.getContent(),
                    message.getCreatedAt(), message.getSenderId(), message.getIsRead());
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
            @AuthenticationPrincipal String userId) {
        try {
            Page<Message> messages = messageService.getMessages(
                    matchId, userId, PageRequest.of(page, size));

            List<MessageResponseDto> content = messages.getContent().stream()
                    .map(m -> new MessageResponseDto(
                            m.getId(), m.getContent(), m.getCreatedAt(),
                            m.getSenderId(), m.getIsRead()))
                    .collect(Collectors.toList());

            Map<String, Object> resp = new HashMap<>();
            resp.put("content", content);
            resp.put("totalPages", messages.getTotalPages());
            resp.put("totalElements", messages.getTotalElements());
            resp.put("currentPage", page);
            return ResponseEntity.ok(resp);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
