package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.CommentDto;
import com.chunshuiquan.backend.dto.CreateMomentRequest;
import com.chunshuiquan.backend.dto.MomentDto;
import com.chunshuiquan.backend.entity.*;
import com.chunshuiquan.backend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/moments")
public class MomentController {

    private final MomentRepository momentRepository;
    private final MomentLikeRepository momentLikeRepository;
    private final MomentCommentRepository momentCommentRepository;
    private final ProfileRepository profileRepository;

    public MomentController(MomentRepository momentRepository,
                            MomentLikeRepository momentLikeRepository,
                            MomentCommentRepository momentCommentRepository,
                            ProfileRepository profileRepository) {
        this.momentRepository = momentRepository;
        this.momentLikeRepository = momentLikeRepository;
        this.momentCommentRepository = momentCommentRepository;
        this.profileRepository = profileRepository;
    }

    /** GET /api/moments — 公开动态时间线 */
    @GetMapping
    public ResponseEntity<List<MomentDto>> timeline(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID myId = UUID.fromString(userId);
        Page<Moment> moments = momentRepository
                .findByIsDeletedFalseAndVisibilityOrderByCreatedAtDesc("public", PageRequest.of(page, size));
        List<MomentDto> dtos = moments.stream().map(m -> toDto(m, myId)).toList();
        return ResponseEntity.ok(dtos);
    }

    /** GET /api/moments/user/{userId} — 某用户的动态 */
    @GetMapping("/user/{targetUserId}")
    public ResponseEntity<List<MomentDto>> userMoments(
            @AuthenticationPrincipal String userId,
            @PathVariable UUID targetUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID myId = UUID.fromString(userId);
        Page<Moment> moments = momentRepository
                .findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(targetUserId, PageRequest.of(page, size));
        List<MomentDto> dtos = moments.stream().map(m -> toDto(m, myId)).toList();
        return ResponseEntity.ok(dtos);
    }

    /** POST /api/moments — 发布动态 */
    @PostMapping
    public ResponseEntity<MomentDto> create(@AuthenticationPrincipal String userId,
                                            @RequestBody CreateMomentRequest request) {
        UUID myId = UUID.fromString(userId);
        if ((request.getContent() == null || request.getContent().isBlank()) &&
            (request.getImageUrls() == null || request.getImageUrls().isEmpty())) {
            return ResponseEntity.badRequest().build();
        }

        Moment moment = new Moment();
        moment.setAuthorId(myId);
        moment.setContent(request.getContent());
        if (request.getImageUrls() != null) {
            moment.setImageUrls(request.getImageUrls().toArray(new String[0]));
        }
        moment.setLocation(request.getLocation());
        moment.setVisibility(request.getVisibility() != null ? request.getVisibility() : "public");
        moment = momentRepository.save(moment);

        return ResponseEntity.ok(toDto(moment, myId));
    }

    /** DELETE /api/moments/{id} — 删除自己的动态 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal String userId,
                                       @PathVariable UUID id) {
        UUID myId = UUID.fromString(userId);
        return momentRepository.findById(id)
                .filter(m -> m.getAuthorId().equals(myId))
                .map(m -> {
                    m.setIsDeleted(true);
                    momentRepository.save(m);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/moments/{id}/like — 点赞/取消点赞 */
    @PostMapping("/{id}/like")
    @Transactional
    public ResponseEntity<Map<String, Object>> toggleLike(@AuthenticationPrincipal String userId,
                                                          @PathVariable UUID id) {
        UUID myId = UUID.fromString(userId);
        Moment moment = momentRepository.findById(id).orElse(null);
        if (moment == null) return ResponseEntity.notFound().build();

        Optional<MomentLike> existing = momentLikeRepository.findByMomentIdAndUserId(id, myId);
        boolean liked;
        if (existing.isPresent()) {
            momentLikeRepository.delete(existing.get());
            moment.setLikeCount(Math.max(0, moment.getLikeCount() - 1));
            liked = false;
        } else {
            MomentLike like = new MomentLike();
            like.setMomentId(id);
            like.setUserId(myId);
            momentLikeRepository.save(like);
            moment.setLikeCount(moment.getLikeCount() + 1);
            liked = true;
        }
        momentRepository.save(moment);

        return ResponseEntity.ok(Map.of("liked", liked, "likeCount", moment.getLikeCount()));
    }

    /** GET /api/moments/{id}/comments — 获取评论列表 */
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<CommentDto>> getComments(@PathVariable UUID id) {
        List<MomentComment> comments = momentCommentRepository.findByMomentIdOrderByCreatedAtAsc(id);
        List<CommentDto> dtos = comments.stream().map(this::toCommentDto).toList();
        return ResponseEntity.ok(dtos);
    }

    /** POST /api/moments/{id}/comments — 发表评论 */
    @PostMapping("/{id}/comments")
    @Transactional
    public ResponseEntity<CommentDto> addComment(@AuthenticationPrincipal String userId,
                                                 @PathVariable UUID id,
                                                 @RequestBody Map<String, String> body) {
        UUID myId = UUID.fromString(userId);
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Moment moment = momentRepository.findById(id).orElse(null);
        if (moment == null) return ResponseEntity.notFound().build();

        MomentComment comment = new MomentComment();
        comment.setMomentId(id);
        comment.setAuthorId(myId);
        comment.setContent(content);
        String replyToId = body.get("replyToId");
        if (replyToId != null && !replyToId.isBlank()) {
            comment.setReplyToId(UUID.fromString(replyToId));
        }
        comment = momentCommentRepository.save(comment);

        moment.setCommentCount(moment.getCommentCount() + 1);
        momentRepository.save(moment);

        return ResponseEntity.ok(toCommentDto(comment));
    }

    // ========== 私有方法 ==========

    private MomentDto toDto(Moment m, UUID viewerId) {
        MomentDto dto = new MomentDto();
        dto.setId(m.getId().toString());
        dto.setAuthorId(m.getAuthorId().toString());
        dto.setContent(m.getContent());
        dto.setImageUrls(m.getImageUrls() != null ? Arrays.asList(m.getImageUrls()) : List.of());
        dto.setLocation(m.getLocation());
        dto.setLikeCount(m.getLikeCount());
        dto.setCommentCount(m.getCommentCount());
        dto.setLikedByMe(momentLikeRepository.existsByMomentIdAndUserId(m.getId(), viewerId));
        dto.setCreatedAt(m.getCreatedAt());

        profileRepository.findById(m.getAuthorId()).ifPresent(p -> {
            dto.setAuthorName(p.getName());
            dto.setAuthorAvatar(p.getAvatarUrls() != null && p.getAvatarUrls().length > 0
                    ? p.getAvatarUrls()[0] : null);
            dto.setAuthorVipTier(p.getVipTier());
        });

        return dto;
    }

    private CommentDto toCommentDto(MomentComment c) {
        CommentDto dto = new CommentDto();
        dto.setId(c.getId().toString());
        dto.setAuthorId(c.getAuthorId().toString());
        dto.setReplyToId(c.getReplyToId() != null ? c.getReplyToId().toString() : null);
        dto.setContent(c.getContent());
        dto.setCreatedAt(c.getCreatedAt());

        profileRepository.findById(c.getAuthorId()).ifPresent(p -> {
            dto.setAuthorName(p.getName());
            dto.setAuthorAvatar(p.getAvatarUrls() != null && p.getAvatarUrls().length > 0
                    ? p.getAvatarUrls()[0] : null);
        });

        return dto;
    }
}
