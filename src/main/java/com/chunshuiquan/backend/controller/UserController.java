package com.chunshuiquan.backend.controller;

import com.chunshuiquan.backend.dto.UpdateProfileRequest;
import com.chunshuiquan.backend.entity.Profile;
import com.chunshuiquan.backend.repository.ProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileRepository profileRepository;

    public UserController(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    // GET /api/users/me — 当前用户信息
    @GetMapping("/me")
    public ResponseEntity<Profile> me(@AuthenticationPrincipal String userId) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(p -> { return ResponseEntity.ok(p); })
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/users/profile — 更新资料
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal String userId,
            @RequestBody UpdateProfileRequest req) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    if (req.getName() != null && !req.getName().isBlank())
                        profile.setName(req.getName());
                    if (req.getBio() != null)
                        profile.setBio(req.getBio());
                    if (req.getJobTitle() != null)
                        profile.setJobTitle(req.getJobTitle());
                    if (req.getLookingFor() != null)
                        profile.setLookingFor(req.getLookingFor());
                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/users/avatar — 上传头像 URL（前端上传到 CDN 后传 URL 过来）
    @PostMapping("/avatar")
    public ResponseEntity<?> addAvatar(
            @AuthenticationPrincipal String userId,
            @RequestBody java.util.Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "url 不能为空"));
        }
        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    List<String> urls = new ArrayList<>(Arrays.asList(
                            profile.getAvatarUrls() != null ? profile.getAvatarUrls() : new String[0]
                    ));
                    urls.add(url);
                    profile.setAvatarUrls(urls.toArray(new String[0]));
                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE /api/users/avatar — 删除头像（按 index）
    @DeleteMapping("/avatar/{index}")
    public ResponseEntity<?> deleteAvatar(
            @AuthenticationPrincipal String userId,
            @PathVariable int index) {
        return profileRepository.findById(UUID.fromString(userId))
                .map(profile -> {
                    List<String> urls = new ArrayList<>(Arrays.asList(
                            profile.getAvatarUrls() != null ? profile.getAvatarUrls() : new String[0]
                    ));
                    if (index < 0 || index >= urls.size()) {
                        return ResponseEntity.badRequest().body(
                                java.util.Map.of("error", "index 超出范围"));
                    }
                    urls.remove(index);
                    profile.setAvatarUrls(urls.toArray(new String[0]));
                    profile = profileRepository.save(profile);
                    return ResponseEntity.ok((Object) profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/users/feed — 推荐卡片列表
    @GetMapping("/feed")
    public ResponseEntity<List<Profile>> feed(
            @AuthenticationPrincipal String userId,
            @RequestParam(defaultValue = "20") int size) {
        UUID myId = UUID.fromString(userId);
        List<Profile> feed = profileRepository.findFeed(myId, PageRequest.of(0, size));
        return ResponseEntity.ok(feed);
    }
}
