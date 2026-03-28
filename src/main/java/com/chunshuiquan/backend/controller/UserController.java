package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileRepository profileRepository;

    public UserController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    // 当前用户信息
    @GetMapping("/me")
    public ResponseEntity<Profile> me(@AuthenticationPrincipal String userId) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 推荐卡片列表（默认20张）
    @GetMapping("/feed")
    public ResponseEntity<List<Profile>> feed(@AuthenticationPrincipal String userId,
                                              @RequestParam(defaultValue = "20") int size) {
        UUID myId = UUID.fromString(userId);
        List<Profile> feed = profileRepository.findFeed(myId, PageRequest.of(0, size));
        // 不返回密码哈希
        feed.forEach(p -> p.setPasswordHash(null));
        return ResponseEntity.ok(feed);
    }
}
