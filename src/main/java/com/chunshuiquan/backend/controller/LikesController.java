package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.entity.Swipe;
import com.chunshuiquan.backend.repository.ProfileRepository;
import com.chunshuiquan.backend.repository.SwipeRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/likes")
public class LikesController {

    private final SwipeRepository swipeRepository;
    private final ProfileRepository profileRepository;

    public LikesController(SwipeRepository swipeRepository,
                           ProfileRepository profileRepository) {
        this.swipeRepository = swipeRepository;
        this.profileRepository = profileRepository;
    }

    /** GET /api/likes/count — 有多少人喜欢我（所有人可见） */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> likesCount(@AuthenticationPrincipal String userId) {
        List<Swipe> likes = swipeRepository.findWhoLikedMe(UUID.fromString(userId));
        return ResponseEntity.ok(Map.of("count", likes.size()));
    }

    /** GET /api/likes — 谁喜欢了我（VIP才返回完整信息，非VIP返回模糊信息） */
    @GetMapping
    public ResponseEntity<?> whoLikesMe(@AuthenticationPrincipal String userId) {
        UUID myId = UUID.fromString(userId);
        Profile me = profileRepository.findById(myId).orElse(null);
        if (me == null) return ResponseEntity.notFound().build();

        List<Swipe> likes = swipeRepository.findWhoLikedMe(myId);
        boolean isVip = !"none".equals(me.getVipTier()) &&
                me.getVipExpiresAt() != null &&
                me.getVipExpiresAt().isAfter(java.time.OffsetDateTime.now());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Swipe swipe : likes) {
            Profile liker = profileRepository.findById(swipe.getSwiperId()).orElse(null);
            if (liker == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", liker.getId().toString());
            item.put("direction", swipe.getDirection());

            if (isVip) {
                // VIP: 完整信息
                item.put("name", liker.getName());
                item.put("age", liker.getBirthDate() != null ?
                        java.time.Period.between(liker.getBirthDate(), java.time.LocalDate.now()).getYears() : null);
                item.put("avatarUrl", liker.getAvatarUrls() != null && liker.getAvatarUrls().length > 0 ?
                        liker.getAvatarUrls()[0] : null);
                item.put("city", liker.getCity());
                item.put("blurred", false);
            } else {
                // 非VIP: 模糊信息
                item.put("name", liker.getName().substring(0, 1) + "**");
                item.put("avatarUrl", liker.getAvatarUrls() != null && liker.getAvatarUrls().length > 0 ?
                        liker.getAvatarUrls()[0] : null);
                item.put("blurred", true);
            }
            result.add(item);
        }

        return ResponseEntity.ok(Map.of("isVip", isVip, "likes", result, "count", result.size()));
    }
}
