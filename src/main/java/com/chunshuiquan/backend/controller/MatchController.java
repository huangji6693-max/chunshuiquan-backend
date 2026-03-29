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
    public ResponseEntity<List<MatchItemDto>> getMyMatches(@AuthenticationPrincipal String userId) {
        Profile me = profileRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Match> matches = matchRepository.findActiveMatchesByUserId(me.getId());
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(72);

        List<MatchItemDto> result = matches.stream().map(match -> {
            UUID otherId = match.getUser1Id().equals(me.getId())
                    ? match.getUser2Id() : match.getUser1Id();

            boolean isNew = match.getCreatedAt().isAfter(threshold)
                    && messageRepository.countByMatchId(match.getId()) == 0;

            MatchItemDto.OtherUserDto otherDto = profileRepository.findById(otherId)
                    .map(other -> new MatchItemDto.OtherUserDto(
                            other.getId(), other.getEmail(), other.getName(),
                            other.getBio(),
                            other.getAvatarUrls() != null ? Arrays.asList(other.getAvatarUrls()) : Collections.emptyList(),
                            other.getJobTitle()))
                    .orElse(null);

            return new MatchItemDto(match.getId(), match.getCreatedAt(), isNew, otherDto);
        }).collect(Collectors.toList());

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
