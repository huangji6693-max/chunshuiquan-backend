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
import java.util.stream.Collectors;

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
        List<MomentDto> dtos = toBatchDto(moments.getContent(), myId);
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
        List<MomentDto> dtos = toBatchDto(moments.getContent(), myId);
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
        List<CommentDto> dtos = toBatchCommentDto(comments);
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

    /** 批量转换动态列表（消除 N+1：一次查所有作者Profile + 一次查所有点赞状态） */
    private List<MomentDto> toBatchDto(List<Moment> moments, UUID viewerId) {
        if (moments.isEmpty()) return List.of();

        // 1) 批量查询所有作者的 Profile（1次SQL）
        List<UUID> authorIds = moments.stream().map(Moment::getAuthorId).distinct().toList();
        Map<UUID, Profile> profileMap = profileRepository.findAllById(authorIds)
                .stream().collect(Collectors.toMap(Profile::getId, p -> p));

        // 2) 批量查询当前用户对这些动态的点赞状态（1次SQL）
        List<UUID> momentIds = moments.stream().map(Moment::getId).toList();
        Set<UUID> likedSet = new HashSet<>(momentLikeRepository.findLikedMomentIds(viewerId, momentIds));

        // 3) 组装 DTO
        return moments.stream().map(m -> {
            MomentDto dto = new MomentDto();
            dto.setId(m.getId().toString());
            dto.setAuthorId(m.getAuthorId().toString());
            dto.setContent(m.getContent());
            dto.setImageUrls(m.getImageUrls() != null ? Arrays.asList(m.getImageUrls()) : List.of());
            dto.setLocation(m.getLocation());
            dto.setLikeCount(m.getLikeCount());
            dto.setCommentCount(m.getCommentCount());
            dto.setLikedByMe(likedSet.contains(m.getId()));
            dto.setCreatedAt(m.getCreatedAt());

            Profile p = profileMap.get(m.getAuthorId());
            if (p != null) {
                dto.setAuthorName(p.getName());
                dto.setAuthorAvatar(p.getAvatarUrls() != null && p.getAvatarUrls().length > 0
                        ? p.getAvatarUrls()[0] : null);
                dto.setAuthorVipTier(p.getVipTier());
            }
            return dto;
        }).toList();
    }

    /** 单条动态转换（用于 create 等只返回一条的场景） */
    private MomentDto toDto(Moment m, UUID viewerId) {
        return toBatchDto(List.of(m), viewerId).get(0);
    }

    /** 批量转换评论列表（消除 N+1：一次查所有评论者Profile） */
    private List<CommentDto> toBatchCommentDto(List<MomentComment> comments) {
        if (comments.isEmpty()) return List.of();

        // 批量查询所有评论者的 Profile（1次SQL）
        List<UUID> authorIds = comments.stream().map(MomentComment::getAuthorId).distinct().toList();
        Map<UUID, Profile> profileMap = profileRepository.findAllById(authorIds)
                .stream().collect(Collectors.toMap(Profile::getId, p -> p));

        return comments.stream().map(c -> {
            CommentDto dto = new CommentDto();
            dto.setId(c.getId().toString());
            dto.setAuthorId(c.getAuthorId().toString());
            dto.setReplyToId(c.getReplyToId() != null ? c.getReplyToId().toString() : null);
            dto.setContent(c.getContent());
            dto.setCreatedAt(c.getCreatedAt());

            Profile p = profileMap.get(c.getAuthorId());
            if (p != null) {
                dto.setAuthorName(p.getName());
                dto.setAuthorAvatar(p.getAvatarUrls() != null && p.getAvatarUrls().length > 0
                        ? p.getAvatarUrls()[0] : null);
            }
            return dto;
        }).toList();
    }

    /** 单条评论转换（用于 addComment 等只返回一条的场景） */
    private CommentDto toCommentDto(MomentComment c) {
        return toBatchCommentDto(List.of(c)).get(0);
    }
}
